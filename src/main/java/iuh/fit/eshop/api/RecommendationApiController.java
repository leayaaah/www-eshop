package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationApiController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<iuh.fit.eshop.entity.Product>> getRecommendations(
            Authentication auth, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "8") int limit) {
        String username = auth == null ? null : auth.getName();
        return ResponseEntity.ok(recommendationService.recommend(username, limit));
    }
}