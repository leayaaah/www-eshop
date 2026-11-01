package iuh.fit.eshop.repository;
import iuh.fit.eshop.ai.RecommendationEventAggregate;

import iuh.fit.eshop.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findTop20ByUsernameAndEventTypeOrderByIdDesc(String username, String eventType);

    List<UserEvent> findByUsernameAndEventTypeAndProductIdNotIn(String username, String eventType, Collection<Long> viewedIds);

    List<UserEvent> findByUsernameAndCreatedAtAfter(String username, LocalDateTime since);

    List<UserEvent> findBySessionIdAndCreatedAtAfter(String sessionId, LocalDateTime since);

    @Query(value = """
            select product_id, event_type, count(*) as event_count,
                   coalesce(sum(value_ms), 0) as dwell_ms, max(created_at_v2) as last_created_at
            from user_event
            where username = :username and created_at_v2 >= :since and product_id is not null
            group by product_id, event_type
            """, nativeQuery = true)
    List<Object[]> aggregateForUser(@Param("username") String username, @Param("since") LocalDateTime since);

    @Query(value = """
            select product_id, event_type, count(*) as event_count,
                   coalesce(sum(value_ms), 0) as dwell_ms, max(created_at_v2) as last_created_at
            from user_event
            where session_id = :sessionId and created_at_v2 >= :since and product_id is not null
            group by product_id, event_type
            """, nativeQuery = true)
    List<Object[]> aggregateForSession(@Param("sessionId") String sessionId, @Param("since") LocalDateTime since);

    boolean existsBySessionIdAndProductIdAndEventTypeAndCreatedAtAfter(
            String sessionId, Long productId, String eventType, LocalDateTime since);

    @Query(value = """
            select e.product_id, e.event_type, date(e.created_at_v2), count(*)
            from user_event e
            left join users u on u.id = e.user_id
            where e.created_at_v2 >= :since
              and e.product_id is not null
              and e.event_type in ('VIEW', 'CLICK', 'DWELL', 'ADD_TO_CART')
              and (e.session_id is null or e.session_id <> 'server')
              and (e.user_id is null or u.role <> 'ROLE_SELLER')
            group by e.product_id, e.event_type, date(e.created_at_v2)
            """, nativeQuery = true)
    List<Object[]> aggregateForHeat(@Param("since") LocalDateTime since);
}