package iuh.fit.eshop.repository;

import iuh.fit.eshop.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
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

    boolean existsBySessionIdAndProductIdAndEventTypeAndCreatedAtAfter(
            String sessionId, Long productId, String eventType, LocalDateTime since);
}