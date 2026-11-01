package iuh.fit.eshop.service;
import iuh.fit.eshop.entity.*;
import iuh.fit.eshop.exception.NotFoundException;
import iuh.fit.eshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import iuh.fit.eshop.ai.ProductChangedEvent;
import java.util.List;
import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class ProductService  {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ApplicationEventPublisher publisher;
    public Page<Product> search(String keyword,Long categoryId,int page,int size) {
        return products.search(keyword==null||keyword.isBlank()?null:keyword.trim(),categoryId,PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),50),Sort.by("id").descending()));
    }
    public Product findById(Long id) {
        return products.findDetailedById(id).orElseThrow(()->new NotFoundException("Không tìm thấy sản phẩm #"+id));
    }
    public List<Product> findAllWithCategory() {
        return products.findAllWithCategory();
    }
    public List<Product> searchProducts(String keyword, String category, BigDecimal maxPrice, int limit) {
        return products.searchProducts(
                        keyword == null || keyword.isBlank() ? null : keyword.trim(),
                        category == null || category.isBlank() ? null : category.trim(),
                        maxPrice,
                        PageRequest.of(0, Math.min(Math.max(1, limit), 5)))
                .getContent();
    }
@PreAuthorize("hasRole('SELLER')")
public Product save(Product p,Long categoryId) {
        p.setCategory(categories.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId)));
        Product saved = products.save(p);
        publisher.publishEvent(new ProductChangedEvent(saved.getId(), ProductChangedEvent.ChangeType.UPSERT));
        return saved;
    }
@PreAuthorize("hasRole('SELLER')")
@Transactional
public void delete(Long id) {
        Product p = findById(id); // throws NotFoundException if not found
        products.delete(p);
        publisher.publishEvent(new ProductChangedEvent(id, ProductChangedEvent.ChangeType.DELETE));
    }
}
