package iuh.fit.eshop.repository;

import iuh.fit.eshop.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findTop20ByUsernameAndEventTypeOrderByIdDesc(String username, String eventType);

    List<UserEvent> findByUsernameAndEventTypeAndProductIdNotIn(String username, String eventType, Collection<Long> viewedIds);
}