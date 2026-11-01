package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTools {
    private final ProductService productService;
    private final ProductVectorSearchService vectorSearch;
    private final RecommendationService recommendations;

    public record ProductInfo(Long id, String name, BigDecimal price, Integer stock, String description, String link) {}

    @Tool(description = "Tìm sản phẩm theo từ khoá, danh mục và giá tối đa; tối đa 5 kết quả.")
    public List<ProductInfo> searchProducts(
            @ToolParam(description = "Từ khoá") String keyword,
            @ToolParam(description = "Tên danh mục, có thể bỏ trống") String category,
            @ToolParam(description = "Giá tối đa, có thể bỏ trống") BigDecimal maxPrice) {
        return vectorSearch.search(keyword, 10).stream()
                .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                .filter(p -> category == null || category.isBlank()
                        || p.getCategory().getName().toLowerCase().contains(category.toLowerCase()))
                .limit(5).map(this::info).toList();
    }

    @Tool(description = "Xem chi tiết một sản phẩm")
    public ProductInfo getProductDetail(@ToolParam(description = "ID sản phẩm") Long productId) {
        return info(productService.findById(productId));
    }

    @Tool(description = "Tìm sản phẩm tương tự theo mô tả nhu cầu")
    public List<ProductInfo> findSimilarProducts(@ToolParam(description = "Mô tả nhu cầu") String description) {
        return vectorSearch.similar(description, 10).stream().map(this::info).toList();
    }

    @Tool(description = "Lấy danh sách sản phẩm được đề xuất thực tế cho người dùng hiện tại")
    public List<ProductInfo> getRecommendedProducts() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null || !auth.isAuthenticated() ? null : auth.getName();
        return recommendations.recommend(username, 8).stream().map(this::info).toList();
    }

    @Tool(description = "Xem sở thích sản phẩm của người dùng hiện tại")
    public String getMyInterests() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "Bạn cần đăng nhập để xem sở thích cá nhân.";
        return "Sở thích được tính từ lịch sử xem, tương tác và mua hàng; hãy dùng danh sách gợi ý trên trang cửa hàng.";
    }

    private ProductInfo info(Product product) {
        String description = product.getDescription() == null ? "" : product.getDescription();
        return new ProductInfo(product.getId(), product.getName(), product.getPrice(), product.getQuantity(),
                description.substring(0, Math.min(200, description.length())), "/products/" + product.getId());
    }
}
