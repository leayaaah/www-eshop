package iuh.fit.eshop.ai;

import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.OrderRepository;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final UserEventRepository events;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ObjectProvider<Cart> cartProvider;
    private final ProductVectorSearchService vectorSearch;
    private final HybridRecommendationService hybrid;
    @Value("${app.reco.algorithm:v2}")
    private String algorithm;

    public List<Product> recommend(String username, int limit) {
        return recommend(username, null, limit);
    }

    public List<Product> recommend(String username, String sessionId, int limit) {
        if ("v2".equalsIgnoreCase(algorithm)) {
            return hybrid.recommend(username, sessionId, limit);
        }
        Map<Long, Double> scores = scoreEvents(username, sessionId);
        Set<Long> excluded = excludedProducts(username);
        List<Product> all = products.findAllWithCategory();
        String query = interestQuery(scores, all);
        List<Product> candidates = vectorSearch.search(query, 40);
        if (candidates.isEmpty()) candidates = all;
        candidates = candidates.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .filter(p -> !excluded.contains(p.getId()))
                .sorted(Comparator.comparingDouble((Product p) -> scores.getOrDefault(p.getId(), 0d)).reversed()
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return diversify(candidates, limit);
    }

    public List<Product> similar(Product product, int limit) {
        if ("v2".equalsIgnoreCase(algorithm)) {
            return hybrid.similar(product, limit);
        }
        List<Product> candidates = vectorSearch.similar(
                product.getName() + ". " + (product.getDescription() == null ? "" : product.getDescription()), 30);
        if (candidates.isEmpty()) candidates = products.findAllWithCategory();
        return diversify(candidates.stream()
                .filter(p -> !Objects.equals(p.getId(), product.getId()) && p.getQuantity() > 0)
                .toList(), limit);
    }

    public void invalidate(String username, String sessionId) {
        hybrid.invalidate(username, sessionId);
    }

    private Map<Long, Double> scoreEvents(String username, String sessionId) {
        List<UserEvent> recent = username != null
                ? events.findByUsernameAndCreatedAtAfter(username, LocalDateTime.now().minusDays(90))
                : sessionId == null ? List.of()
                : events.findBySessionIdAndCreatedAtAfter(sessionId, LocalDateTime.now().minusDays(90));
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

    private String interestQuery(Map<Long, Double> scores, List<Product> all) {
        if (scores.isEmpty()) return "sản phẩm phổ biến trong cửa hàng";
        return scores.entrySet().stream().sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10).map(Map.Entry::getKey)
                .map(id -> all.stream().filter(p -> id.equals(p.getId())).findFirst()
                        .map(p -> p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription()))
                        .orElse(""))
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("sản phẩm phổ biến");
    }

    private double dwellWeight(Integer valueMs) {
        if (valueMs == null) return 0;
        if (valueMs >= 60_000) return 3;
        if (valueMs >= 30_000) return 2;
        return valueMs >= 10_000 ? 1 : 0;
    }

    private Set<Long> excludedProducts(String username) {
        Set<Long> excluded = new HashSet<>();
        if (username != null) orders.findForUserByUsername(username).stream()
                .flatMap(o -> o.getDetails().stream()).map(d -> d.getProduct().getId()).forEach(excluded::add);
        Cart cart = cartProvider.getIfAvailable();
        if (cart != null) excluded.addAll(cart.getItems().keySet());
        return excluded;
    }

    private List<Product> diversify(List<Product> source, int limit) {
        int max = Math.max(1, Math.min(limit, 20));
        Map<Long, Integer> counts = new HashMap<>();
        List<Product> result = new ArrayList<>();
        for (Product product : source) {
            if (product.getCategory() == null) continue;
            long categoryId = product.getCategory().getId();
            if (counts.getOrDefault(categoryId, 0) >= 3) continue;
            result.add(product);
            counts.merge(categoryId, 1, Integer::sum);
            if (result.size() >= max) break;
        }
        return result;
    }
}
