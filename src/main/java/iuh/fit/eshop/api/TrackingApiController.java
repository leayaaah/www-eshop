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

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingApiController {
    private final TrackingService tracking;

    public record Event(@NotNull Long productId, @NotNull EventType type,
                        @Min(1000) @Max(1_800_000) Integer valueMs) {}

    @PostMapping
    public ResponseEntity<Void> track(@Valid @RequestBody Event event, HttpServletRequest request) {
        if (event.type() != EventType.CLICK && event.type() != EventType.DWELL) {
            return ResponseEntity.badRequest().build();
        }
        if (event.type() == EventType.DWELL && event.valueMs() == null) {
            return ResponseEntity.badRequest().build();
        }
        tracking.record(request, event.productId(), event.type(), event.valueMs());
        return ResponseEntity.noContent().build();
    }
}