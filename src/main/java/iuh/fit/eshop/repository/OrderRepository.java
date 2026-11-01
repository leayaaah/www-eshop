package iuh.fit.eshop.repository;
import iuh.fit.eshop.entity.ShopOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
public interface OrderRepository extends JpaRepository<ShopOrder,Long>  {
    @EntityGraph(attributePaths={"details","details.product","user"}) List<ShopOrder> findAllByOrderByIdDesc();
    @EntityGraph(attributePaths={"details","details.product","user"})
@Query("select distinct o from ShopOrder o where o.user.id=:userId order by o.id desc") List<ShopOrder> findForUser(@Param("userId") Long userId);
    @EntityGraph(attributePaths={"details","details.product","user"})
    @Query("select distinct o from ShopOrder o where o.user.username=:username order by o.id desc")
    List<ShopOrder> findForUserByUsername(@Param("username") String username);
    @EntityGraph(attributePaths={"details","details.product","user"})
@Query("select distinct o from ShopOrder o where o.id=:id") Optional<ShopOrder> findDetailedById(@Param("id") Long id);

@Query(value = """
        select d.product_id, coalesce(sum(d.quantity), 0)
        from order_detail d join orders o on o.id = d.order_id
        where o.order_date >= :since
        group by d.product_id
        """, nativeQuery = true)
List<Object[]> salesSince(@Param("since") LocalDateTime since);

@Query(value = """
    select d.product_id, date(o.order_date), coalesce(sum(d.quantity), 0)
    from order_detail d join orders o on o.id = d.order_id
    left join users u on u.id = o.user_id
    where o.order_date >= :since
      and (o.user_id is null or u.role <> 'ROLE_SELLER')
    group by d.product_id, date(o.order_date)
    """, nativeQuery = true)
List<Object[]> salesByDaySince(@Param("since") LocalDateTime since);
}
