package iuh.fit.eshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @Column(name = "event_id", unique = true)
    private String eventId;

    @Column(name = "anonymous_id")
    private String anonymousId;

    private String username;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "product_id")
    private Long productId;

    private String page;

    @Column(name = "value_ms")
    private Integer valueMs;

    private String source;

    @Column(name = "consent_state")
    private String consentState;

    @Column(name = "schema_version")
    private String schemaVersion;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "created_at_v2")
    private LocalDateTime createdAt;
}