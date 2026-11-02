package iuh.fit.eshop.api;

import iuh.fit.eshop.tracking.EventType;
import iuh.fit.eshop.tracking.TrackingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingApiController {
    private final TrackingService tracking;

    public record Event(@NotNull String eventId, String sessionId, String anonymousId,
                        @NotNull Long productId, @NotNull EventType type,
                        @Min(1000) @Max(1_800_000) Integer valueMs,
                        String consentState, String schemaVersion, String contextJson,
                        LocalDateTime timestamp) {}

    @PostMapping
    public ResponseEntity<Void> track(@Valid @RequestBody Event event, HttpServletRequest request) {
        if (event.type() != EventType.CLICK && event.type() != EventType.DWELL) {
            return ResponseEntity.badRequest().build();
        }
        if (event.type() == EventType.DWELL && event.valueMs() == null) {
            return ResponseEntity.badRequest().build();
        }
        tracking.recordClient(event.eventId(), event.sessionId(), event.anonymousId(), event.productId(),
                event.type(), event.valueMs(), event.consentState(), event.schemaVersion(),
                event.contextJson(), event.timestamp());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> trackBatch(@RequestBody List<Event> events, HttpServletRequest request) {
        if (events == null || events.size() > 50) return ResponseEntity.badRequest().build();
        for (Event event : events) {
            if (event == null || (event.type() != EventType.CLICK && event.type() != EventType.DWELL)
                    || event.valueMs() == null && event.type() == EventType.DWELL) {
                return ResponseEntity.badRequest().build();
            }
            tracking.recordClient(event.eventId(), event.sessionId(), event.anonymousId(), event.productId(),
                    event.type(), event.valueMs(), event.consentState(), event.schemaVersion(),
                    event.contextJson(), event.timestamp());
        }
        return ResponseEntity.noContent().build();
    }
}