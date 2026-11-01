package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.OrderRepository;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductHeatServiceTest {
    @Test
    void appliesDecayAndLeavesZeroIntensityWhenThereAreNoEvents() {
        UserEventRepository events = mock(UserEventRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Product product = product(1L);
        when(events.aggregateForHeat(any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, "VIEW", java.sql.Date.valueOf(LocalDate.now()), 2L}));
        when(orders.salesByDaySince(any())).thenReturn(List.of());
        when(products.findAllWithCategory()).thenReturn(List.of(product));

        ProductHeatService service = new ProductHeatService(events, orders, products);
        ProductHeatService.HeatResult result = service.heat(ProductHeatService.Metric.VIEWS, 30);

        assertEquals(2, result.points().get(0).views());
        assertTrue(result.points().get(0).raw() > 0);
        ProductHeatService.HeatResult empty = new ProductHeatService(events, orders, products)
                .heat(ProductHeatService.Metric.PURCHASES, 7);
        assertEquals(0d, empty.points().get(0).intensity());
    }

    @Test
    void conversionRequiresFiveViews() {
        UserEventRepository events = mock(UserEventRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        when(events.aggregateForHeat(any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, "VIEW", java.sql.Date.valueOf(LocalDate.now()), 4L}));
        when(orders.salesByDaySince(any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, java.sql.Date.valueOf(LocalDate.now()), 1L}));
        when(products.findAllWithCategory()).thenReturn(List.of(product(1L)));

        ProductHeatService.HeatPoint point = new ProductHeatService(events, orders, products)
                .heat(ProductHeatService.Metric.CONVERSION, 30).points().get(0);

        assertEquals(0d, point.conversion());
    }

    private Product product(Long id) {
        Category category = new Category();
        category.setName("Test");
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(BigDecimal.ONE);
        product.setCategory(category);
        return product;
    }
}
