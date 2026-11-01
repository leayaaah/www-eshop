package iuh.fit.eshop.ai;

import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.OrderRepository;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRecommendationService {
    private static final double HALF_LIFE_DAYS = 14d;
    private static final Duration CACHE_TTL = Duration.ofMinutes(3);
    private static final int MAX_PER_EVENT_TYPE = 6;
    private static final int MAX_PER_PRODUCT = 12;

    private final UserEventRepository events;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ObjectProvider<Cart> cartProvider;
    private final ProductVectorIndex vectorIndex;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public List<Product> recommend(String username, String sessionId, int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        String identity = username == null ? "session:" + sessionId : "user:" + username;
        String key = identity + ":" + size;
        CachedResult cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(LocalDateTime.now())) {
            return cached.products().stream().limit(size).toList();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(username == null ? 7 : 90);
        List<RecommendationEventAggregate> aggregates = aggregates(username, sessionId, since);
        Map<Long, Double> behavior = behaviorScores(aggregates, now);
        Map<Long, Double> categoryInterest = categoryInterest(behavior);
        Map<Long, Double> popularity = popularity(now);
        Set<Long> excluded = excludedProducts(username);
        List<Product> all = products.findAllWithCategory();
        List<Product> candidates = all.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .filter(p -> !excluded.contains(p.getId()))
                .toList();
        float[] interest = interestVector(behavior, all);
        Map<Long, Double> similarities = new HashMap<>();
        for (Product candidate : candidates) {
            similarities.put(candidate.getId(), vectorIndex.cosine(interest, vectorIndex.vector(candidate.getId())));
        }
        Map<Long, Double> sim = normalize(similarities);
        Map<Long, Double> cat = normalize(categoryInterestForProducts(categoryInterest, candidates));
        Map<Long, Double> pop = normalize(popularityForProducts(popularity, candidates));
        Map<Long, Double> beh = normalize(behaviorForProducts(behavior, candidates));
        boolean hasSimilarity = vectorIndex.available() && interest != null;

        List<ScoredProduct> scored = candidates.stream()
                .map(p -> new ScoredProduct(p, finalScore(p, sim, cat, pop, beh, hasSimilarity)))
                .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed()
                        .thenComparing((ScoredProduct s) -> popularity.getOrDefault(s.product().getId(), 0d),
                                Comparator.reverseOrder())
                        .thenComparing(s -> s.product().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(s -> s.product().getId()))
                .toList();
        List<Product> result = diversify(scored, size, popularity, categoryInterest);
        cache.put(key, new CachedResult(result, now.plus(CACHE_TTL)));
        log.info("Recommendation source={}, user={}, candidates={}, result={}, elapsedMs={}",
                hasSimilarity ? "memory" : "fallback", key, candidates.size(), result.size(),
                Duration.between(now, LocalDateTime.now()).toMillis());
        return result;
    }

    public List<Product> similar(Product product, int limit) {
        List<Product> all = products.findAllWithCategory();
        List<Product> result = vectorIndex.similar(product, limit, all);
        if (!result.isEmpty()) return result;
        return all.stream()
                .filter(p -> !Objects.equals(p.getId(), product.getId()))
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .filter(p -> p.getCategory().getId().equals(product.getCategory().getId()))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    public void invalidate(String username, String sessionId) {
        String identity = username == null ? "session:" + sessionId : "user:" + username;
        cache.keySet().removeIf(key -> key.startsWith(identity + ":"));
    }

    private List<RecommendationEventAggregate> aggregates(String username, String sessionId, LocalDateTime since) {
        List<Object[]> rows = username == null
                ? (sessionId == null ? List.of() : events.aggregateForSession(sessionId, since))
                : events.aggregateForUser(username, since);
        return rows.stream().map(row -> new RecommendationEventAggregate(
                ((Number) row[0]).longValue(), String.valueOf(row[1]),
                ((Number) row[2]).longValue(), ((Number) row[3]).longValue(),
                row[4] instanceof java.sql.Timestamp timestamp
                        ? timestamp.toLocalDateTime() : (LocalDateTime) row[4])).toList();
    }

    private Map<Long, Double> behaviorScores(List<RecommendationEventAggregate> rows, LocalDateTime now) {
        Map<Long, Double> result = new HashMap<>();
        Map<Long, Integer> productTotals = new HashMap<>();
        for (RecommendationEventAggregate row : rows) {
            int count = (int) Math.min(row.eventCount(), MAX_PER_EVENT_TYPE);
            double weight = switch (row.eventType()) {
                case "CLICK" -> 1d;
                case "VIEW" -> 2d;
                case "DWELL" -> dwellWeight(row.dwellMs() / Math.max(1, count));
                case "ADD_TO_CART" -> 4d;
                case "PURCHASE" -> 6d;
                default -> 0d;
            };
            double ageDays = Math.max(0, Duration.between(row.lastCreatedAt(), now).toHours() / 24d);
            double score = count * weight * Math.pow(0.5, ageDays / HALF_LIFE_DAYS);
            long productId = row.productId();
            double remaining = Math.max(0, MAX_PER_PRODUCT - productTotals.getOrDefault(productId, 0));
            double bounded = Math.min(score, remaining);
            result.merge(productId, bounded, Double::sum);
            productTotals.merge(productId, (int) Math.ceil(bounded), Integer::sum);
        }
        return result;
    }

    private double dwellWeight(long valueMs) {
        if (valueMs >= 60_000) return 3;
        if (valueMs >= 30_000) return 2;
        return valueMs >= 10_000 ? 1 : 0;
    }

    private Map<Long, Double> categoryInterest(Map<Long, Double> behavior) {
        Map<Long, Double> result = new HashMap<>();
        List<Product> all = products.findAllWithCategory();
        double total = behavior.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return result;
        for (var entry : behavior.entrySet()) {
            all.stream().filter(p -> p.getId().equals(entry.getKey())).findFirst()
                    .ifPresent(p -> result.merge(p.getCategory().getId(), entry.getValue() / total, Double::sum));
        }
        return result;
    }

    private Map<Long, Double> popularity(LocalDateTime now) {
        Map<Long, Double> result = new HashMap<>();
        for (Object[] row : orders.salesSince(now.minusDays(30))) {
            result.put(((Number) row[0]).longValue(), Math.log1p(((Number) row[1]).doubleValue()));
        }
        return result;
    }

    private float[] interestVector(Map<Long, Double> behavior, List<Product> all) {
        List<Map.Entry<Long, Double>> top = behavior.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(10).toList();
        float[] result = null;
        for (var entry : top) {
            float[] vector = vectorIndex.vector(entry.getKey());
            if (vector == null) continue;
            if (result == null) result = new float[vector.length];
            if (result.length != vector.length) continue;
            for (int i = 0; i < result.length; i++) result[i] += vector[i] * entry.getValue().floatValue();
        }
        if (result == null) return null;
        double squared = 0d;
        for (float value : result) squared += value * value;
        double norm = Math.sqrt(squared);
        if (norm == 0) return null;
        for (int i = 0; i < result.length; i++) result[i] /= (float) norm;
        return result;
    }

    private Map<Long, Double> categoryInterestForProducts(Map<Long, Double> interests, List<Product> candidates) {
        Map<Long, Double> result = new HashMap<>();
        for (Product p : candidates) result.put(p.getId(), interests.getOrDefault(p.getCategory().getId(), 0d));
        return result;
    }

    private Map<Long, Double> popularityForProducts(Map<Long, Double> popularity, List<Product> candidates) {
        Map<Long, Double> result = new HashMap<>();
        for (Product p : candidates) result.put(p.getId(), popularity.getOrDefault(p.getId(), 0d));
        return result;
    }

    private Map<Long, Double> behaviorForProducts(Map<Long, Double> behavior, List<Product> candidates) {
        Map<Long, Double> result = new HashMap<>();
        for (Product p : candidates) result.put(p.getId(), behavior.getOrDefault(p.getId(), 0d));
        return result;
    }

    private double finalScore(Product product, Map<Long, Double> sim, Map<Long, Double> cat,
                              Map<Long, Double> pop, Map<Long, Double> beh, boolean hasSimilarity) {
        if (hasSimilarity) {
            return .50 * sim.getOrDefault(product.getId(), 0d)
                    + .20 * cat.getOrDefault(product.getId(), 0d)
                    + .15 * pop.getOrDefault(product.getId(), 0d)
                    + .15 * beh.getOrDefault(product.getId(), 0d);
        }
        double total = .20 + .15 + .15;
        return (.20 * cat.getOrDefault(product.getId(), 0d)
                + .15 * pop.getOrDefault(product.getId(), 0d)
                + .15 * beh.getOrDefault(product.getId(), 0d)) / total;
    }

    private List<Product> diversify(List<ScoredProduct> scored, int limit, Map<Long, Double> popularity,
                                    Map<Long, Double> categoryInterest) {
        if (scored.isEmpty()) return List.of();
        int exploration = Math.min(2, limit / 4);
        Set<Long> viewedCategories = categoryInterest.keySet();
        List<Product> result = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        int relevanceSlots = Math.max(0, limit - exploration);
        while (result.size() < relevanceSlots) {
            ScoredProduct best = null;
            double bestMmr = -Double.MAX_VALUE;
            for (ScoredProduct candidate : scored) {
                if (used.contains(candidate.product().getId())
                        || countCategory(result, candidate.product()) >= 4) continue;
                double redundancy = result.stream()
                        .mapToDouble(selected -> vectorIndex.cosine(
                                vectorIndex.vector(candidate.product().getId()),
                                vectorIndex.vector(selected.getId())))
                        .max().orElse(0d);
                double mmr = .7 * candidate.score() - .3 * redundancy;
                if (best == null || mmr > bestMmr) {
                    best = candidate;
                    bestMmr = mmr;
                }
            }
            if (best == null) break;
            result.add(best.product());
            used.add(best.product().getId());
        }
        scored.stream()
                .filter(candidate -> !viewedCategories.contains(candidate.product().getCategory().getId()))
                .sorted(Comparator.comparingDouble((ScoredProduct s) ->
                        popularity.getOrDefault(s.product().getId(), 0d)).reversed())
                .filter(candidate -> !used.contains(candidate.product().getId()))
                .filter(candidate -> countCategory(result, candidate.product()) < 4)
                .limit(exploration)
                .forEach(candidate -> {
                    result.add(candidate.product());
                    used.add(candidate.product().getId());
                });
        for (ScoredProduct candidate : scored) {
            if (result.size() >= limit) break;
            if (!used.contains(candidate.product().getId()) && countCategory(result, candidate.product()) < 4) {
                result.add(candidate.product());
                used.add(candidate.product().getId());
            }
        }
        return result;
    }

    private long countCategory(List<Product> products, Product product) {
        return products.stream().filter(p -> p.getCategory().getId().equals(product.getCategory().getId())).count();
    }

    private Map<Long, Double> normalize(Map<Long, Double> source) {
        if (source.isEmpty()) return source;
        double min = source.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = source.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max <= min) return source.keySet().stream().collect(HashMap::new, (m, k) -> m.put(k, 0d), HashMap::putAll);
        return source.entrySet().stream().collect(HashMap::new,
                (m, e) -> m.put(e.getKey(), (e.getValue() - min) / (max - min)), HashMap::putAll);
    }

    private Set<Long> excludedProducts(String username) {
        Set<Long> excluded = new HashSet<>();
        if (username != null) orders.findForUserByUsername(username).stream()
                .flatMap(o -> o.getDetails().stream()).map(d -> d.getProduct().getId()).forEach(excluded::add);
        Cart cart = cartProvider.getIfAvailable();
        if (cart != null) excluded.addAll(cart.getItems().keySet());
        return excluded;
    }

    private record ScoredProduct(Product product, double score) {}
    private record CachedResult(List<Product> products, LocalDateTime expiresAt) {}
}
