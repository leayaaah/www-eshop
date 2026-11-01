package iuh.fit.eshop.service;
import iuh.fit.eshop.entity.*;
import iuh.fit.eshop.exception.NotFoundException;
import iuh.fit.eshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ProductService  {
    private final ProductRepository products;
    private final CategoryRepository categories;
    public Page<Product> search(String keyword,Long categoryId,int page,int size) {
        return products.search(keyword==null||keyword.isBlank()?null:keyword.trim(),categoryId,PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),50),Sort.by("id").descending()));
    }
    public Product findById(Long id) {
        return products.findDetailedById(id).orElseThrow(()->new NotFoundException("Không tìm thấy sản phẩm #"+id));
    }
@PreAuthorize("hasRole('SELLER')")
public Product save(Product p,Long categoryId) {
        p.setCategory(categories.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId)));
        return products.save(p);
    }
@PreAuthorize("hasRole('SELLER')")
@Transactional
public void delete(Long id) {
        Product p = findById(id); // throws NotFoundException if not found
        products.delete(p);
    }
}
