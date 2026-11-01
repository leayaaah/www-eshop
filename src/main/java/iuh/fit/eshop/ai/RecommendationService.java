package iuh.fit.eshop.ai;

import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.OrderRepository;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final UserEventRepository events;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ObjectProvider<Cart> cartProvider;

    public List<Product> recommend(String username, int limit) {
        List<Product> all = products.findAllWithCategory();
        Set<Long> excluded = excludedProducts(username);
        Map<Long, Double> scores = scoreEvents(username);
        Set<Long> interests = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10).map(Map.Entry::getKey).collect(Collectors.toSet());

        List<Product> ranked = all.stream()
                .filter(p -> p.getQuantity() > 0 && !excluded.contains(p.getId()))
                .sorted(Comparator
                        .comparingDouble((Product p) -> relatedScore(p, all, interests, scores)).reversed()
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return diversify(ranked, limit);
    }

    public List<Product> similar(Product product, int limit) {
        List<Product> candidates = products.findAllWithCategory().stream()
                .filter(p -> !Objects.equals(p.getId(), product.getId()) && p.getQuantity() > 0)
                .sorted(Comparator.comparing((Product p) ->
                        Objects.equals(p.getCategory().getId(), product.getCategory().getId()) ? 0 : 1)
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return diversify(candidates, limit);
    }

    private Map<Long, Double> scoreEvents(String username) {
        List<UserEvent> recent = username == null
                ? List.of()
                : events.findByUsernameAndCreatedAtAfter(username, LocalDateTime.now().minusDays(90));
        Map<Long, Double> scores = new HashMap<>();
        for (UserEvent event : recent) {
            if (event.getProductId() == null) continue;
            double weight = switch (event.getEventType()) {
                case "CLICK" -> 1;
                case "VIEW" -> 2;
                case "DWELL" -> dwellWeight(event.getValueMs());
                case "ADD_TO_CART" -> 4;
                case "PURCHASE" -> 6;
                default -> 0;
            };
            double ageDays = event.getCreatedAt() == null ? 0 :
                    Math.max(0, java.time.Duration.between(event.getCreatedAt(), LocalDateTime.now()).toHours() / 24d);
            scores.merge(event.getProductId(), weight * Math.pow(0.5, ageDays / 14), Double::sum);
        }
        return scores;
    }

    private double dwellWeight(Integer valueMs) {
        if (valueMs == null) return 0;
        if (valueMs >= 60_000) return 3;
        if (valueMs >= 30_000) return 2;
        return valueMs >= 10_000 ? 1 : 0;
    }

    private double relatedScore(Product product, List<Product> all, Set<Long> interests, Map<Long, Double> scores) {
        if (scores.containsKey(product.getId())) return 0;
        return all.stream().filter(p -> interests.contains(p.getId())
                        && Objects.equals(p.getCategory().getId(), product.getCategory().getId()))
                .mapToDouble(p -> scores.getOrDefault(p.getId(), 0d)).sum();
    }

    private Set<Long> excludedProducts(String username) {
        Set<Long> excluded = new HashSet<>();
        if (username != null) {
            orders.findForUserByUsername(username).stream()
                    .flatMap(o -> o.getDetails().stream())
                    .map(d -> d.getProduct().getId()).forEach(excluded::add);
        }
        Cart cart = cartProvider.getIfAvailable();
        if (cart != null) excluded.addAll(cart.getItems().keySet());
        return excluded;
    }

    private List<Product> diversify(List<Product> products, int limit) {
        Map<Long, Integer> counts = new HashMap<>();
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            long categoryId = product.getCategory().getId();
            if (counts.getOrDefault(categoryId, 0) >= 3) continue;
            result.add(product);
            counts.merge(categoryId, 1, Integer::sum);
            if (result.size() >= Math.max(1, Math.min(limit, 20))) break;
        }
        return result;
    }
}
