package iuh.fit.eshop.controller.admin;

import iuh.fit.eshop.ai.ProjectionService;
import iuh.fit.eshop.ai.ProductHeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/products/map")
@RequiredArgsConstructor
public class ProductMapController {
    private final ProjectionService projection;
    private final ProductHeatService heat;

    @GetMapping("/data")
    public ProductMapResponse data() {
        ProjectionService.ProjectionResult result = projection.project();
        List<MissingProduct> missing = result.missing().stream()
                .map(product -> new MissingProduct(product.getId(), product.getName()))
                .toList();
        return new ProductMapResponse(result.points(), result.points().size(),
                result.explainedVariance(), missing);
    }

    @GetMapping("/heat")
    public ProductHeatService.HeatResult heat(
            @RequestParam(defaultValue = "INTEREST") String metric,
            @RequestParam(defaultValue = "30") int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng thời gian phải là 7, 30 hoặc 90 ngày");
        }
        try {
            return heat.heat(ProductHeatService.Metric.valueOf(metric.trim().toUpperCase()), days);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ số không hợp lệ");
        }
    }

    public record ProductMapResponse(List<ProjectionService.ProductMapPoint> points,
                                      int productCount, double explainedVariance,
                                      List<MissingProduct> missingVectors) {}

    public record MissingProduct(Long id, String name) {}
}
