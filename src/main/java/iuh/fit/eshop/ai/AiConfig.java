package iuh.fit.eshop.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 MessageChatMemoryAdvisor memoryAdvisor) {
        return builder
                .defaultSystem("Bạn là trợ lý của IUH eShop. Trả lời tiếng Việt, ngắn gọn.\n" +
                        "Chỉ nói về sản phẩm và chính sách của cửa hàng.\n" +
                        "Giá và tồn kho phải lấy từ công cụ, không tự bịa.\n" +
                        "Trước khi đặt hàng phải hỏi khách xác nhận.")
                .defaultAdvisors(memoryAdvisor)
                .build();
    }

    /** SearchRequest dùng chung để truy vấn chính sách (RAG). */
    public static final SearchRequest POLICY_SEARCH = SearchRequest.builder()
            .topK(4)
            .similarityThreshold(0.5)
            .filterExpression("source == 'policy'")
            .build();

    /** SearchRequest dùng chung để truy vấn sản phẩm (semantic). */
    public static final SearchRequest PRODUCT_SEARCH = SearchRequest.builder()
            .topK(8)
            .similarityThreshold(0.3)
            .filterExpression("source == 'product'")
            .build();
}
