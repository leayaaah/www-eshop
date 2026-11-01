package iuh.fit.eshop.api;

import iuh.fit.eshop.ai.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {
    private final ChatbotService chatbot;
    private final ConcurrentHashMap<String, java.util.ArrayDeque<Instant>> requests = new ConcurrentHashMap<>();

    public record ChatRequest(@NotBlank @Size(max = 500) String message,
                              @Size(max = 100) String conversationId) {}
    public record ChatResponse(String answer) {}

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest req, Authentication auth,
                                  HttpServletRequest request) {
        String owner = auth == null || !auth.isAuthenticated()
                ? "session:" + request.getSession(true).getId()
                : "user:" + auth.getName();
        String conversation = req.conversationId() == null || req.conversationId().isBlank()
                ? owner : owner + ":" + req.conversationId().trim();
        var now = Instant.now();
        var history = requests.computeIfAbsent(owner, ignored -> new java.util.ArrayDeque<>());
        synchronized (history) {
            while (!history.isEmpty() && history.peekFirst().isBefore(now.minusSeconds(600))) history.removeFirst();
            if (history.size() >= 20)
                return ResponseEntity.status(429).body(Map.of("error", "Bạn đã đạt giới hạn tin nhắn, hãy thử lại sau."));
            history.addLast(now);
        }
        try {
            Flux<ServerSentEvent<String>> stream = chatbot.stream(req.message().trim(), conversation)
                    .map(chunk -> ServerSentEvent.builder(chunk).build());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(stream);
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Xin lỗi, chatbot hiện không khả dụng."));
        }
    }
}
