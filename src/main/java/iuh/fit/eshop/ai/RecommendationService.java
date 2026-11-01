package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.entity.UserEvent;
import iuh.fit.eshop.repository.UserEventRepository;
import iuh.fit.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final VectorStore vectorStore;
    private final ChatClient chat;
    private final UserEventRepository events;
    private final ProductRepository products;

    public String recommend(String username) {
        List<Long> viewedIds = events
                .findTop20ByUsernameAndEventTypeOrderByIdDesc(username, "view_product")
                .stream()
                .map(UserEvent::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (viewedIds.isEmpty()) return "";

        List<Product> viewed = products.findAllById(viewedIds);

        String profile = "Khách quan tâm: " + viewed.stream()
                .map(p -> p.getName() + " (" + p.getPrice() + " đồng)")
                .collect(Collectors.joining("; "));

        var req = SearchRequest.builder()
                .query(profile)
                .topK(3)
                .filterExpression("source == 'product'")
                .build();

        String candidates = vectorStore.similaritySearch(req).stream()
                .map(d -> {
                    Object productId = d.getMetadata().get("productId");
                    return d.getText() + (productId != null ? " (productId=" + productId + ")" : "");
                })
                .collect(Collectors.joining("\n- ", "- ", ""));

        return chat.prompt()
                .system("Chỉ gợi ý trong danh sách ứng viên, nêu lý do ngắn gọn bằng tiếng Việt.")
                .user("Hồ sơ: " + profile + "\nỨng viên:\n" + candidates)
                .call()
                .content();
    }
}