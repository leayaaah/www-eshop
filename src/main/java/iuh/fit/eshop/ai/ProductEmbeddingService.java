package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEmbeddingService {
    private final ObjectProvider<VectorStore> vectorStores;
    private final iuh.fit.eshop.repository.ProductRepository products;

    @Async
    @EventListener
    public void onProductChanged(ProductChangedEvent event) {
        VectorStore store = vectorStores.getIfAvailable();
        if (store == null) return;
        if (event.type() == ProductChangedEvent.ChangeType.DELETE) {
            delete(event.productId(), store);
            return;
        }
        products.findDetailedById(event.productId()).ifPresent(product -> upsert(product, store));
    }

    public void upsert(Product product) {
        VectorStore store = vectorStores.getIfAvailable();
        if (store != null) upsert(product, store);
    }

    private void upsert(Product product, VectorStore store) {
        String text = content(product);
        String hash = hash(text);
        var existing = store.similaritySearch(SearchRequest.builder()
                .query(text).topK(10)
                .filterExpression("source == 'product' && productId == " + product.getId())
                .build());
        if (existing.stream().anyMatch(d -> hash.equals(d.getMetadata().get("contentHash")))) return;
        existing.stream().map(Document::getId).forEach(id -> store.delete(List.of(id)));
        store.add(List.of(new Document(text, Map.of(
                "source", "product",
                "productId", product.getId(),
                "categoryId", product.getCategory().getId(),
                "contentHash", hash))));
    }

    private void delete(Long productId, VectorStore store) {
        var existing = store.similaritySearch(SearchRequest.builder()
                .query(String.valueOf(productId)).topK(20)
                .filterExpression("source == 'product' && productId == " + productId)
                .build());
        existing.stream().map(Document::getId).forEach(id -> store.delete(List.of(id)));
    }

    private String content(Product p) {
        return String.join(". ", p.getName(),
                p.getDescription() == null ? "" : p.getDescription(),
                "Danh mục " + p.getCategory().getName(),
                "Giá " + p.getPrice() + " đồng");
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo content hash", ex);
        }
    }
}
