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
import java.util.Arrays;
import java.util.Comparator;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;

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
    @Value("${app.ai.chat-timeout-ms:25000}")
    private long chatTimeoutMillis;
    private static final ExecutorService CHAT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "chatbot-gemini");
        thread.setDaemon(true);
        return thread;
    });

    public String chat(String message, String conversationId) {
        String quickAnswer = quickProductSearch(message);
        if (quickAnswer != null) return quickAnswer;
        String localProductAnswer = localSemanticProductSearch(message);
        if (localProductAnswer != null) return localProductAnswer;

        ChatClient client = clients.getIfAvailable();
        if (client != null && memories.getIfAvailable() != null) {
            Future<String> request = null;
            try {
                request = CHAT_EXECUTOR.submit(() -> {
                    ToolCallBudget.start();
                    try {
                        return client.prompt().user(message).tools(productTools, orderTools)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .call().content();
                    } finally {
                        ToolCallBudget.clear();
                    }
                });
                String answer = request.get(chatTimeoutMillis, TimeUnit.MILLISECONDS);
                if (answer != null && !answer.isBlank()) return answer;
                log.warn("Gemini returned empty content; using product fallback");
            } catch (TimeoutException ex) {
                if (request != null) request.cancel(true);
                log.warn("Gemini request timed out after {} ms; using product fallback", chatTimeoutMillis);
            } catch (InterruptedException ex) {
                if (request != null) request.cancel(true);
                Thread.currentThread().interrupt();
                log.warn("Gemini request interrupted; using product fallback");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                if (cause instanceof ToolCallBudget.ToolCallLimitExceededException) {
                    log.warn("Gemini exceeded the {}-tool-call request budget; using product fallback", ToolCallBudget.MAX_CALLS);
                } else {
                    log.warn("Gemini API failed; using product fallback: {}", cause.getMessage());
                }
            }
        } else {
            log.warn("Gemini ChatClient or ChatMemory is unavailable; using product fallback");
        }
        return productFallback(message);
    }

    private String productFallback(String message) {
        List<Product> products = vectorSearch.search(message, 5);
        if (products.isEmpty()) return "Tôi chưa tìm thấy sản phẩm phù hợp. Bạn có thể thử thêm từ khóa hoặc danh mục.";
        return "Tôi chưa thể hoàn tất câu trả lời AI lúc này. Bạn có thể tham khảo: " +
                products.stream().map(p -> p.getName() + " (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
    }

    private String localSemanticProductSearch(String message) {
        if (message == null) return null;
        String normalized = message.toLowerCase(Locale.ROOT);
        if (!containsProductIntent(normalized)) return null;

        BigDecimal maxPrice = extractMaxPrice(normalized);
        String category = normalized.contains("laptop") ? "laptop" : null;
        List<Product> products = vectorSearch.search(message, 8).stream()
                .filter(product -> maxPrice == null || product.getPrice().compareTo(maxPrice) <= 0)
                .filter(product -> category == null
                        || product.getCategory().getName().toLowerCase(Locale.ROOT).contains(category))
                .limit(5)
                .toList();
        if (products.isEmpty()) return null;

        return "Mình tìm thấy " + products.size() + " sản phẩm phù hợp: " +
                products.stream()
                        .map(p -> p.getName() + " - " + p.getPrice() + " đồng (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + "; " + b).orElse("");
    }

    private boolean containsProductIntent(String message) {
        return message.contains("laptop") || message.contains("máy tính")
                || message.contains("điện thoại") || message.contains("tai nghe")
                || message.contains("mua") || message.contains("tư vấn");
    }

    private BigDecimal extractMaxPrice(String message) {
        Matcher million = Pattern.compile("(?:tối đa|dưới|khoảng|ngân sách)\\s*(?:là|tầm)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr)").matcher(message);
        if (million.find()) {
            return BigDecimal.valueOf(Double.parseDouble(million.group(1).replace(',', '.')))
                    .multiply(BigDecimal.valueOf(1_000_000));
        }
        Matcher exact = Pattern.compile("(?:tối đa|dưới|khoảng|ngân sách)\\s*(?:là|tầm)?\\s*([\\d.,]+)\\s*(?:đ|đồng|vnd)?").matcher(message);
        if (exact.find()) {
            String value = exact.group(1).replace(".", "").replace(",", "");
            return new BigDecimal(value);
        }
        return null;
    }

    private String quickProductSearch(String message) {
        if (message == null || message.length() > 100) return null;
        String query = message.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        Set<String> stopWords = Set.of("tôi", "mình", "muốn", "cần", "mua", "tìm", "cho", "xem",
                "sản", "phẩm", "một", "vài", "loại", "có", "gì", "nào", "giúp", "với", "giá",
                "bao", "nhiêu", "không", "xin", "hãy", "tư", "vấn", "chào", "hello", "hi");
        String keyword = Arrays.stream(query.split(" "))
                .filter(word -> !stopWords.contains(word))
                .reduce((a, b) -> a + " " + b).orElse("");
        if (keyword.isBlank()) return null;

        List<String> tokens = Arrays.stream(keyword.split(" "))
                .filter(token -> token.length() > 1)
                .toList();
        List<Product> allProducts = productService.findAllWithCategory();
        List<Product> products = allProducts.stream()
                .filter(product -> containsAllTokens(product, tokens))
                .sorted(Comparator.comparingInt((Product product) -> matchScore(product, tokens)).reversed()
                        .thenComparing(Product::getId))
                .limit(5)
                .toList();
        if (products.isEmpty()) {
            return "Mình chưa tìm thấy sản phẩm phù hợp với \"" + keyword
                    + "\". Bạn có thể thử từ khóa khác như điện thoại, laptop hoặc tai nghe.";
        }
        return "Mình tìm thấy " + products.size() + " sản phẩm phù hợp: " +
                products.stream()
                        .map(p -> p.getName() + " - " + p.getPrice() + " đồng (/products/" + p.getId() + ")")
                        .reduce((a, b) -> a + "; " + b).orElse("");
    }

    private boolean containsAllTokens(Product product, List<String> tokens) {
        String searchable = searchableText(product);
        return tokens.stream().allMatch(searchable::contains);
    }

    private int matchScore(Product product, List<String> tokens) {
        String name = product.getName().toLowerCase(java.util.Locale.ROOT);
        String category = product.getCategory().getName().toLowerCase(java.util.Locale.ROOT);
        return (int) tokens.stream().mapToInt(token ->
                (name.contains(token) ? 3 : 0) + (category.contains(token) ? 2 : 0)).sum();
    }

    private String searchableText(Product product) {
        String category = product.getCategory().getName() + " "
                + (product.getCategory().getDescription() == null ? "" : product.getCategory().getDescription());
        return (product.getName() + " " + product.getDescription() + " " + category)
                .toLowerCase(java.util.Locale.ROOT);
    }
}
