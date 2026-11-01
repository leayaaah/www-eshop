package iuh.fit.eshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_event")
@Getter
@Setter
public class UserEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    private String username;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "product_id")
    private Long productId;

    private String page;
}