package iuh.fit.eshop.repository;
import iuh.fit.eshop.entity.ShopOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<ShopOrder,Long>  {
    @EntityGraph(attributePaths={"details","details.product","user"}) List<ShopOrder> findAllByOrderByIdDesc();
    @EntityGraph(attributePaths={"details","details.product","user"})
@Query("select distinct o from ShopOrder o where o.user.id=:userId order by o.id desc") List<ShopOrder> findForUser(@Param("userId") Long userId);
    @EntityGraph(attributePaths={"details","details.product","user"})
@Query("select distinct o from ShopOrder o where o.id=:id") Optional<ShopOrder> findDetailedById(@Param("id") Long id);
}
