package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.ProductSearchService;
import iuh.fit.eshop.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchApiController {
    private final ProductSearchService search;

    @GetMapping("/semantic")
    public ResponseEntity<List<ProductDto>> semantic(@RequestParam @NotBlank @Size(max = 200) String q) {
        return ResponseEntity.ok(search.semanticSearch(q, 8).stream()
                .map(ProductDto::from)
                .toList());
    }

    public record ProductDto(Long id, String name, String description, BigDecimal price, Integer stock) {
        public static ProductDto from(Product p) {
            return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getQuantity());
        }
    }
}