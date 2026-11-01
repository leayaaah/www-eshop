package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.ProductTools;
import iuh.fit.eshop.ai.OrderTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ChatApiController {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ProductTools productTools;
    private final OrderTools orderTools;
    private final ConcurrentHashMap<String, java.util.ArrayDeque<Instant>> requests = new ConcurrentHashMap<>();

    public record ChatRequest(@NotBlank @Size(max = 500) String message) {}

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody ChatRequest req, Authentication auth) {
        String conversation = auth == null ? "guest" : auth.getName();
        var now = Instant.now();
        var history = requests.computeIfAbsent(conversation, ignored -> new java.util.ArrayDeque<>());
        synchronized (history) {
            while (!history.isEmpty() && history.peekFirst().isBefore(now.minusSeconds(600))) history.removeFirst();
            if (history.size() >= 20) return ResponseEntity.status(429).body(Map.of("error", "Bạn đã đạt giới hạn tin nhắn, hãy thử lại sau."));
            history.addLast(now);
        }
        try {
            String answer = chatClient.prompt().user(req.message()).tools(productTools, orderTools)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversation)).call().content();
            return ResponseEntity.ok(Map.of("answer", answer == null ? "Xin lỗi, tôi chưa có câu trả lời." : answer));
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Xin lỗi, chatbot hiện không khả dụng."));
        }
    }
}