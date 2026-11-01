package iuh.fit.eshop.controller.admin;

import iuh.fit.eshop.ai.ProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/products/map")
@RequiredArgsConstructor
public class ProductMapController {
    private final ProjectionService projection;

    @GetMapping("/data")
    public ProductMapResponse data() {
        ProjectionService.ProjectionResult result = projection.project();
        List<MissingProduct> missing = result.missing().stream()
                .map(product -> new MissingProduct(product.getId(), product.getName()))
                .toList();
        return new ProductMapResponse(result.points(), result.points().size(),
                result.explainedVariance(), missing);
    }

    public record ProductMapResponse(List<ProjectionService.ProductMapPoint> points,
                                      int productCount, double explainedVariance,
                                      List<MissingProduct> missingVectors) {}

    public record MissingProduct(Long id, String name) {}
}
