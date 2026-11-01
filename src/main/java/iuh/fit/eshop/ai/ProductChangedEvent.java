package iuh.fit.eshop.ai;

public record ProductChangedEvent(Long productId, ChangeType type) {
    public enum ChangeType { UPSERT, DELETE }
}
