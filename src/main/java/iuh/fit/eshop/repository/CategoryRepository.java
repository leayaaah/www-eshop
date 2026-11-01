package iuh.fit.eshop.repository;
import iuh.fit.eshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CategoryRepository extends JpaRepository<Category,Long>  {
    boolean existsByNameIgnoreCase(String name);
}
