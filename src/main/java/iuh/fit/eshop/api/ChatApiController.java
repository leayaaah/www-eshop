package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.ProductTools;
import iuh.fit.eshop.ai.OrderTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ProductTools productTools;
    private final OrderTools orderTools;

    public record ChatRequest(@NotBlank @Size(max = 500) String message) {}

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody ChatRequest req, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        String answer = chatClient.prompt()
                .user(req.message())
                .tools(productTools, orderTools)
                .toolContext(Map.of("username", Objects.requireNonNull(auth.getName())))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, Objects.requireNonNull(auth.getName())))
                .call().content();
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}