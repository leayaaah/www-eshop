package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final VectorStore vectorStore;
    private final ProductService productService;

    public List<Product> semanticSearch(String query, int limit) {
        var request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(0.3)
                .filterExpression("source == 'product'")
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(d -> {
                    Object productId = d.getMetadata().get("productId");
                    if (productId == null) {
                        return null;
                    }
                    return ((Number) productId).longValue();
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(productId -> {
                    try {
                        return productService.findById(productId);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
