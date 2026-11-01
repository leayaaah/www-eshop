package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import iuh.fit.eshop.service.ProductService;

import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    private final ObjectProvider<ChatClient> clients;
    private final ObjectProvider<ChatMemory> memories;
    private final ProductTools productTools;
    private final OrderTools orderTools;
    private final ProductVectorSearchService vectorSearch;
    private final ProductService productService;

    public String chat(String message, String conversationId) {
        String quickAnswer = quickProductSearch(message);
        if (quickAnswer != null) return quickAnswer;

        ChatClient client = clients.getIfAvailable();
        if (client != null && memories.getIfAvailable() != null) {
            try {
                String answer = client.prompt().user(message).tools(productTools, orderTools)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .call().content();
                return answer == null || answer.isBlank() ? "Xin lỗi, tôi chưa có câu trả lời." : answer;
            } catch (RuntimeException ex) {
                log.warn("Chat provider failed; using local product fallback: {}", ex.getMessage());
            }
        }
        List<Product> products = vectorSearch.search(message, 5);
        if (products.isEmpty()) return "Chatbot AI hiện chưa được cấu hình. Bạn có thể tìm sản phẩm bằng từ khóa trên cửa hàng.";
        return "Chatbot AI hiện chưa được cấu hình. Bạn có thể tham khảo: " +
                products.stream().map(p -> p.getName() + " (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
    }

    private String quickProductSearch(String message) {
        if (message == null || message.length() > 100) return null;
        String query = message.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        Set<String> stopWords = Set.of("tôi", "mình", "muốn", "cần", "mua", "tìm", "cho", "xem",
                "sản", "phẩm", "một", "vài", "loại", "có", "gì", "nào", "giúp", "với", "giá",
                "bao", "nhiêu", "không", "xin", "hãy", "tư", "vấn");
        String keyword = java.util.Arrays.stream(query.split(" "))
                .filter(word -> !stopWords.contains(word))
                .reduce((a, b) -> a + " " + b).orElse("");
        if (keyword.isBlank()) return null;

        List<Product> products = productService.search(keyword, null, 0, 5).getContent();
        if (products.isEmpty()) return null;
        return "Mình tìm thấy " + products.size() + " sản phẩm phù hợp: " +
                products.stream()
                        .map(p -> p.getName() + " - " + p.getPrice() + " đồng (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + "; " + b).orElse("");
    }
}
