package iuh.fit.eshop.tracking;

import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.ProductRepository;
import iuh.fit.eshop.repository.UserEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {
    private final UserEventRepository events;
    private final ProductRepository products;

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
        saveAsync("server", username, productId, type, null);
    }

    @Async("trackingExecutor")
    public void saveAsync(String sessionId, String username, Long productId, EventType type, Integer valueMs) {
        try {
            var event = new UserEvent();
            event.setSessionId(sessionId);
            event.setUsername(username);
            event.setProductId(productId);
            event.setEventType(type.name());
            event.setValueMs(valueMs);
            event.setCreatedAt(LocalDateTime.now());
            events.save(event);
        } catch (RuntimeException ex) {
            log.warn("Unable to persist tracking event {} for product {}", type, productId, ex);
        }
    }

    private String authenticationName(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        return authentication == null || !authentication.isAuthenticated()
                ? null : authentication.getName();
    }
}
