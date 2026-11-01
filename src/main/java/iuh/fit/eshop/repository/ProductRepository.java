package iuh.fit.eshop.repository;
import iuh.fit.eshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long>  {
    @Query(value="select p from Product p join fetch p.category where (:keyword is null or lower(p.name) like lower(concat('%',:keyword,'%'))) and (:categoryId is null or p.category.id=:categoryId)", countQuery="select count(p) from Product p where (:keyword is null or lower(p.name) like lower(concat('%',:keyword,'%'))) and (:categoryId is null or p.category.id=:categoryId)")
    Page<Product> search(@Param("keyword") String keyword,@Param("categoryId") Long categoryId,Pageable pageable);
    @EntityGraph(attributePaths="category")
    @Query("select p from Product p where p.id=:id") Optional<Product> findDetailedById(@Param("id") Long id);
    @EntityGraph(attributePaths="category")
    @Query("select p from Product p join fetch p.category")
    List<Product> findAllWithCategory();

    @EntityGraph(attributePaths="category")
    @Query("""
            select p from Product p join fetch p.category c
            where (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.description) like lower(concat('%', :keyword, '%')))
              and (:category is null or :category = '' or lower(c.name) like lower(concat('%', :category, '%')))
              and (:maxPrice is null or p.price <= :maxPrice)
            order by p.id desc
            """)
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("category") String category,
                                 @Param("maxPrice") java.math.BigDecimal maxPrice,
                                 Pageable pageable);
}
