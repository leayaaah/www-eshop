package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.OrderRepository;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProductHeatService {
    private static final double HALF_LIFE_DAYS = 14d;
    private static final long CACHE_MILLIS = 5 * 60 * 1000L;
    private final UserEventRepository events;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final Map<CacheKey, CachedHeat> cache = new ConcurrentHashMap<>();

    public HeatResult heat(Metric metric, int days) {
        CacheKey key = new CacheKey(metric, days);
        CachedHeat cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.createdAt < CACHE_MILLIS) {
            return cached.result;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(days);
        Map<Long, Counters> counters = new HashMap<>();
        for (Object[] row : events.aggregateForHeat(since)) {
            Long productId = ((Number) row[0]).longValue();
            String type = String.valueOf(row[1]);
            LocalDate date = toDate(row[2]);
            double weight = eventWeight(type);
            double score = weight * decay(date, now);
            Counters counter = counters.computeIfAbsent(productId, ignored -> new Counters());
            counter.add(type, ((Number) row[3]).longValue(), score);
        }
        for (Object[] row : orders.salesByDaySince(since)) {
            Long productId = ((Number) row[0]).longValue();
            LocalDate date = toDate(row[1]);
            Counters counter = counters.computeIfAbsent(productId, ignored -> new Counters());
            long quantity = ((Number) row[2]).longValue();
            counter.purchases += quantity * decay(date, now);
            counter.purchaseCount += quantity;
        }

        List<Product> all = products.findAllWithCategory().stream().limit(50).toList();
        List<HeatPoint> points = all.stream().map(product -> {
            Counters counter = counters.getOrDefault(product.getId(), new Counters());
            double raw = counter.raw(metric);
            double conversion = counter.viewCount >= 5 ? (double) counter.purchaseCount / counter.viewCount : 0d;
            return new HeatPoint(product.getId(), raw, 0d, counter.viewCount, counter.cartCount,
                    counter.purchaseCount, conversion, product.getCategory().getName());
        }).toList();
        double max = points.stream().mapToDouble(HeatPoint::raw).max().orElse(0d);
        double[] sorted = points.stream().mapToDouble(HeatPoint::raw).sorted().toArray();
        List<HeatPoint> normalized = points.stream()
                .map(point -> point.withIntensity(intensity(point.raw(), max, sorted)))
                .toList();
        Map<String, CategoryHeat> categories = new LinkedHashMap<>();
        for (HeatPoint point : normalized) {
            CategoryHeat previous = categories.get(point.category());
            if (previous == null) {
                categories.put(point.category(), new CategoryHeat(point.category(), point.raw(), point.intensity(), 1));
            } else {
                categories.put(point.category(), new CategoryHeat(point.category(),
                        previous.raw() + point.raw(), previous.intensity() + point.intensity(),
                        previous.productCount() + 1));
            }
        }
        HeatResult result = new HeatResult(normalized, List.copyOf(categories.values()), metric, days,
                normalized.stream().mapToDouble(HeatPoint::views).sum(),
                normalized.stream().mapToDouble(HeatPoint::carts).sum(),
                normalized.stream().mapToDouble(HeatPoint::purchases).sum());
        cache.put(key, new CachedHeat(System.currentTimeMillis(), result));
        return result;
    }

    private double eventWeight(String type) {
        return switch (type) {
            case "CLICK" -> 1d;
            case "VIEW" -> 2d;
            case "DWELL" -> 1d;
            case "ADD_TO_CART" -> 4d;
            default -> 0d;
        };
    }

    private double decay(LocalDate date, LocalDateTime now) {
        long age = Math.max(0, ChronoUnit.HOURS.between(date.atStartOfDay(), now)) / 24;
        return Math.pow(0.5, age / HALF_LIFE_DAYS);
    }

    private double intensity(double raw, double max, double[] sorted) {
        if (max <= 0d || raw <= 0d) return 0d;
        int rank = Arrays.binarySearch(sorted, raw);
        if (rank < 0) rank = -rank - 1;
        double percentile = sorted.length <= 1 ? 1d : (double) rank / (sorted.length - 1);
        return Math.max(.05d, Math.max(raw / max, percentile));
    }

    private LocalDate toDate(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    public enum Metric { INTEREST, VIEWS, CARTS, PURCHASES, CONVERSION }

    public record HeatPoint(Long productId, double raw, double intensity, double views,
                            double carts, double purchases, double conversion, String category) {
        HeatPoint withIntensity(double value) {
            return new HeatPoint(productId, raw, value, views, carts, purchases, conversion, category);
        }
    }

    public record CategoryHeat(String category, double raw, double intensity, int productCount) {}

    public record HeatResult(List<HeatPoint> points, List<CategoryHeat> categories,
                             Metric metric, int days, double views, double carts, double purchases) {}

    private record CacheKey(Metric metric, int days) {}
    private record CachedHeat(long createdAt, HeatResult result) {}

    static class Counters {
        double views;
        double carts;
        double purchases;
        long purchaseCount;
        double interest;

        void add(String type, long count, double score) {
            switch (type) {
                case "VIEW" -> {
                    views += score;
                    viewCount += count;
                }
                case "ADD_TO_CART" -> {
                    carts += score;
                    cartCount += count;
                }
                default -> { }
            }
            interest += score;
        }

        double raw(Metric metric) {
            return switch (metric) {
                case INTEREST -> interest + purchases * 6d;
                case VIEWS -> views;
                case CARTS -> carts;
                case PURCHASES -> purchases;
                case CONVERSION -> viewCount >= 5 ? (double) purchaseCount / viewCount : 0d;
            };
        }

        long viewCount;
        long cartCount;
    }
}
