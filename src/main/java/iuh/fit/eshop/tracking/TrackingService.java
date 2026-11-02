package iuh.fit.eshop.tracking;

import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import iuh.fit.eshop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {
    private final UserEventRepository events;
    private final ProductRepository products;
    private final UserRepository users;

    public void record(HttpServletRequest request, Long productId, EventType type, Integer valueMs) {
        if (!products.existsById(productId)) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        String sessionId = request.getSession(true).getId();
        String username = authenticationName(request);
        if (type == EventType.CLICK &&
                events.existsBySessionIdAndProductIdAndEventTypeAndCreatedAtAfter(
                        sessionId, productId, type.name(), LocalDateTime.now().minusSeconds(2))) {
            return;
        }
        saveAsync(sessionId, username, productId, type, valueMs);
    }

    public void recordServer(HttpServletRequest request, Long productId, EventType type) {
        record(request, productId, type, null);
    }

    public void recordServer(String username, Long productId, EventType type) {
        saveAsync(new EventData(UUID.randomUUID().toString(), "server", null, username,
                productId, type, null, "server", "server", "1.0", null, LocalDateTime.now()));
    }

    @Async("trackingExecutor")
    public void saveAsync(String sessionId, String username, Long productId, EventType type, Integer valueMs) {
        saveAsync(new EventData(UUID.randomUUID().toString(), sessionId, null, username,
                productId, type, valueMs, "server", "server", "1.0", null, LocalDateTime.now()));
    }

    public void recordClient(String eventId, String sessionId, String anonymousId, Long productId,
                             EventType type, Integer valueMs, String consentState, String schemaVersion,
                             String contextJson, LocalDateTime occurredAt) {
        if (!"analytics".equals(consentState)) {
            throw new IllegalArgumentException("Analytics consent is required");
        }
        if (!products.existsById(productId)) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại");
        }
        if (events.existsByEventId(eventId)) {
            return;
        }
        saveAsync(new EventData(eventId, sessionId, anonymousId, null, productId, type, valueMs,
                "client", consentState, schemaVersion, contextJson,
                occurredAt == null ? LocalDateTime.now() : occurredAt));
    }

    private record EventData(String eventId, String sessionId, String anonymousId, String username,
                             Long productId, EventType type, Integer valueMs, String source,
                             String consentState, String schemaVersion, String contextJson,
                             LocalDateTime occurredAt) {}

    @Async("trackingExecutor")
    void saveAsync(EventData data) {
        try {
            var event = new UserEvent();
            event.setEventId(data.eventId());
            event.setSessionId(data.sessionId());
            event.setAnonymousId(data.anonymousId());
            event.setUsername(data.username());
            if (data.username() != null) {
                users.findByUsername(data.username()).ifPresent(user -> event.setUserId(user.getId()));
            }
            event.setProductId(data.productId());
            event.setEventType(data.type().name());
            event.setValueMs(data.valueMs());
            event.setSource(data.source());
            event.setConsentState(data.consentState());
            event.setSchemaVersion(data.schemaVersion());
            event.setContextJson(data.contextJson());
            event.setOccurredAt(data.occurredAt());
            event.setCreatedAt(LocalDateTime.now());
            events.save(event);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateEventId(ex)) {
                log.debug("Tracking event {} was already persisted", data.eventId());
                return;
            }
            log.warn("Unable to persist tracking event {} for product {}", data.type(), data.productId(), ex);
        } catch (RuntimeException ex) {
            log.warn("Unable to persist tracking event {} for product {}", data.type(), data.productId(), ex);
        }
    }

    private boolean isDuplicateEventId(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null
                    && cause.getMessage().contains("ux_user_event_event_id")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String authenticationName(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        return authentication == null || !authentication.isAuthenticated()
                ? null : authentication.getName();
    }
}
