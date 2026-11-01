package iuh.fit.eshop.service;
import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.exception.NotFoundException;
import iuh.fit.eshop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CategoryService  {
    private final CategoryRepository repo;
    public List<Category> findAll() {
        return repo.findAll();
    }
    public Category findById(Long id) {
        return repo.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy danh mục #"+id));
    }
@PreAuthorize("hasRole('SELLER')")
public Category save(Category category) {
        return repo.save(category);
    }
@PreAuthorize("hasRole('SELLER')")
public void delete(Long id) {
        Category c=findById(id);
        if(!c.getProducts().isEmpty()) throw new IllegalStateException("Danh mục đang có sản phẩm");
        repo.delete(c);
    }
}
