package iuh.fit.eshop.ai;

import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProductTools {
    private final ProductService productService;
    private final VectorStore vectorStore;

    public record ProductInfo(Long id, String name, BigDecimal price, Integer stock, String description, String link) {}

    @Tool(description = "Tìm sản phẩm theo từ khoá, tối đa 5 kết quả. Trả về giá và tồn kho.")
    public List<ProductInfo> searchProducts(
            @ToolParam(description = "Từ khoá tên sản phẩm") String keyword) {
        return productService.search(keyword, null, 0, 10).getContent().stream()
                .map(this::info)
                .toList();
    }

    @Tool(description = "Xem chi tiết một sản phẩm")
    public ProductInfo getProductDetail(@ToolParam(description = "ID sản phẩm") Long productId) {
        return info(productService.findById(productId));
    }

    @Tool(description = "Tìm sản phẩm tương tự theo mô tả")
    public List<ProductInfo> findSimilarProducts(@ToolParam(description = "Mô tả nhu cầu") String description) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(description).topK(10)
                        .filterExpression("source == 'product'").build()).stream()
                .map(d -> d.getMetadata().get("productId"))
                .filter(Number.class::isInstance)
                .map(id -> productService.findById(((Number) id).longValue()))
                .map(this::info).toList();
    }

    @Tool(description = "Xem các sản phẩm người dùng đang quan tâm")
    public String getMyInterests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "Bạn cần đăng nhập để xem sở thích cá nhân."
                : "Hãy dùng phần gợi ý trên trang cửa hàng để xem các sản phẩm phù hợp với lịch sử của bạn.";
    }

    private ProductInfo info(iuh.fit.eshop.entity.Product product) {
        String description = product.getDescription() == null ? "" : product.getDescription();
        return new ProductInfo(product.getId(), product.getName(), product.getPrice(), product.getQuantity(),
                description.substring(0, Math.min(200, description.length())), "/products/" + product.getId());
    }
}