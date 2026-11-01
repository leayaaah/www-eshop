package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationApiController {

    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<Map<String, String>> getRecommendations(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        String username = auth.getName();
        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Username không hợp lệ"));
        }
        String recommendation = recommendationService.recommend(username);
        return ResponseEntity.ok(Map.of("recommendation", recommendation != null ? recommendation : "Không có gợi ý"));
    }
}