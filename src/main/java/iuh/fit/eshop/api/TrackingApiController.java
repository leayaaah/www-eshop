package iuh.fit.eshop.api;

import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.UserEventRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingApiController {

    private final UserEventRepository events;

    // Batch event insert: sessionId, list of {type, page, productId}
    public record Ev(String type, String page, Long productId) {}

    public record Batch(@NotBlank String sessionId, @Size(max = 20) List<Ev> events) {}

    @PostMapping
    public ResponseEntity<Void> track(@Valid @RequestBody Batch b, Authentication auth) {
        // Lấy username từ JWT trên server, không tin client gửi về
        String user = auth != null ? auth.getName() : null;
        b.events().forEach(e -> {
            UserEvent u = new UserEvent();
            u.setSessionId(b.sessionId());
            u.setUsername(user);
            u.setEventType(e.type());
            u.setProductId(e.productId());
            u.setPage(e.page());
            events.save(u);
        });
        return ResponseEntity.ok().build();
    }
}