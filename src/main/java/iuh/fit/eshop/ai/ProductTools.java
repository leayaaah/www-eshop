package iuh.fit.eshop.ai;

import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTools {
    private final ProductService productService;

    public record ProductInfo(Long id, String name, BigDecimal price, Integer stock) {}

    @Tool(description = "Tìm sản phẩm theo từ khoá, tối đa 5 kết quả. Trả về giá và tồn kho.")
    public List<ProductInfo> searchProducts(
            @ToolParam(description = "Từ khoá tên sản phẩm") String keyword) {
        return productService.search(keyword, null, 0, 5).getContent().stream()
                .map(p -> new ProductInfo(p.getId(), p.getName(), p.getPrice(), p.getQuantity()))
                .toList();
    }
}