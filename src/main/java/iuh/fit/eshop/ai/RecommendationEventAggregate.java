package iuh.fit.eshop.ai;

public record RecommendationEventAggregate(
        Long productId,
        String eventType,
        long eventCount,
        long dwellMs,
        java.time.LocalDateTime lastCreatedAt) {
}
