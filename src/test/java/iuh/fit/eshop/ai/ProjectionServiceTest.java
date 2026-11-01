package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProjectionServiceTest {
    @Test
    void separatesClustersAndProducesStableCoordinates() {
        ProductVectorIndex index = mock(ProductVectorIndex.class);
        ProductRepository repository = mock(ProductRepository.class);
        ProjectionService service = new ProjectionService(index, repository);

        List<Product> products = List.of(
                product(1L, "A1", "A"), product(2L, "A2", "A"), product(3L, "B1", "B"),
                product(4L, "B2", "B"), product(5L, "C1", "C"), product(6L, "C2", "C"));
        Map<Long, float[]> vectors = Map.of(
                1L, new float[]{10, 0, 0}, 2L, new float[]{9, 1, 0},
                3L, new float[]{0, 10, 0}, 4L, new float[]{1, 9, 0},
                5L, new float[]{0, 0, 10}, 6L, new float[]{0, 1, 9});

        ProjectionService.ProjectionResult first = service.calculate(products, vectors);
        ProjectionService.ProjectionResult second = service.calculate(products, vectors);

        assertEquals(6, first.points().size());
        assertTrue(first.explainedVariance() >= 0d && first.explainedVariance() <= 1d);
        assertEquals(first.points(), second.points());
        assertTrue(first.points().stream().allMatch(point -> point.x() >= 0d && point.x() <= 1d
                && point.y() >= 0d && point.y() <= 1d));
    }

    @Test
    void reportsProductsWithoutVectors() {
        ProductVectorIndex index = mock(ProductVectorIndex.class);
        ProductRepository repository = mock(ProductRepository.class);
        ProjectionService service = new ProjectionService(index, repository);
        Product present = product(1L, "Có vector", "A");
        Product absent = product(2L, "Thiếu vector", "B");

        ProjectionService.ProjectionResult result = service.calculate(
                List.of(present, absent), Map.of(1L, new float[]{1, 0}));

        assertEquals(List.of(absent), result.missing());
        assertEquals(1, result.points().size());
    }

    private Product product(Long id, String name, String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(BigDecimal.ONE);
        product.setCategory(category);
        return product;
    }
}
