package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductVectorSearchService {
    private final ObjectProvider<VectorStore> vectorStores;
    private final ProductService products;

    public List<Product> search(String query, int limit) {
        VectorStore store = vectorStores.getIfAvailable();
        if (query == null || query.isBlank()) return List.of();
        if (store == null) {
            return products.search(query, null, 0, limit).getContent();
        }
        List<Product> result = store.similaritySearch(SearchRequest.builder()
                        .query(query.trim())
                        .topK(Math.max(1, Math.min(limit, 50)))
                        .similarityThreshold(0.3)
                        .filterExpression("source == 'product'")
                        .build())
                .stream()
                .map(this::productId)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::safeFind)
                .filter(Objects::nonNull)
                .toList();
        return result.isEmpty() ? products.search(query, null, 0, limit).getContent() : result;
    }

    public List<Product> similar(String description, int limit) {
        return search(description, limit);
    }

    private Long productId(Document document) {
        Object value = document.getMetadata().get("productId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private Product safeFind(Long id) {
        try {
            return products.findById(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
