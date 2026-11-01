package iuh.fit.eshop.repository;
import iuh.fit.eshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long>  {
    @Query(value="select p from Product p join fetch p.category where (:keyword is null or lower(p.name) like lower(concat('%',:keyword,'%'))) and (:categoryId is null or p.category.id=:categoryId) and (:minPrice is null or p.price >= :minPrice) and (:maxPrice is null or p.price <= :maxPrice) and (:inStock = false or p.quantity > 0)",
           countQuery="select count(p) from Product p where (:keyword is null or lower(p.name) like lower(concat('%',:keyword,'%'))) and (:categoryId is null or p.category.id=:categoryId) and (:minPrice is null or p.price >= :minPrice) and (:maxPrice is null or p.price <= :maxPrice) and (:inStock = false or p.quantity > 0)")
    Page<Product> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                         @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                         @Param("inStock") boolean inStock, Pageable pageable);
    @EntityGraph(attributePaths="category")
    @Query("select p from Product p where p.id=:id") Optional<Product> findDetailedById(@Param("id") Long id);
    @EntityGraph(attributePaths="category")
    @Query("select p from Product p join fetch p.category")
    List<Product> findAllWithCategory();
}
