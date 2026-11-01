package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private final ObjectProvider<ChatClient> clients;
    private final ObjectProvider<ChatMemory> memories;
    private final ProductTools productTools;
    private final OrderTools orderTools;
    private final ProductVectorSearchService vectorSearch;

    public String chat(String message, String conversationId) {
        ChatClient client = clients.getIfAvailable();
        if (client != null && memories.getIfAvailable() != null) {
            String answer = client.prompt().user(message).tools(productTools, orderTools)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call().content();
            return answer == null || answer.isBlank() ? "Xin lỗi, tôi chưa có câu trả lời." : answer;
        }
        List<Product> products = vectorSearch.search(message, 5);
        if (products.isEmpty()) return "Chatbot AI hiện chưa được cấu hình. Bạn có thể tìm sản phẩm bằng từ khóa trên cửa hàng.";
        return "Chatbot AI hiện chưa được cấu hình. Bạn có thể tham khảo: " +
                products.stream().map(p -> p.getName() + " (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
    }
}
