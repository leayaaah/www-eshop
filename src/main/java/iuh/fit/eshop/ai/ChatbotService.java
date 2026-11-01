package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.entity.ShopOrder;
import iuh.fit.eshop.service.OrderService;
import iuh.fit.eshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "muon", "can", "mua", "tim", "cho", "xem", "san", "pham",
            "một", "vài", "loại", "có", "gì", "nào", "giúp", "với", "giá", "bao",
            "nhieu", "khong", "xin", "hay", "tu", "van", "chiec", "dang", "uu", "tien",
            "mot", "vai", "loai", "co", "gi", "nao", "giup", "voi", "gia", "nhan");
    private static final Pattern MILLION_PRICE = Pattern.compile(
            "(?:toi da|duoi|khoang|ngan sach)\\s*(?:la|tam)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr)");

    private final ProductService productService;
    private final OrderService orderService;

    public String chat(String message, String conversationId) {
        long startedAt = System.nanoTime();
        String normalized = normalize(message);
        String answer = orderAnswer(normalized);
        if (answer == null) answer = policyAnswer(normalized);
        if (answer == null) answer = productAnswer(normalized);
        if (answer == null) {
            answer = "Mình có thể hỗ trợ tìm sản phẩm, tư vấn giá/danh mục hoặc xem đơn hàng của bạn.";
        }
        log.info("Chat request stage=local-sql durationMs={} conversationIdPresent={}",
                elapsedMs(startedAt), conversationId != null && !conversationId.isBlank());
        return answer;
    }

    public Flux<String> stream(String message, String conversationId) {
        return Flux.just(chat(message, conversationId));
    }

    private String orderAnswer(String message) {
        if (!containsAny(message, "don hang", "don cua toi", "trang thai don", "ma don")) return null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "Bạn cần đăng nhập để xem đơn hàng của mình.";
        }
        List<ShopOrder> orders = orderService.mine(authentication.getName());
        if (orders.isEmpty()) return "Bạn chưa có đơn hàng nào.";
        return "Các đơn hàng gần đây: " + orders.stream().limit(5)
                .map(order -> "#" + order.getId() + " - " + order.getStatus().name()
                        + " - " + order.getTotalAmount() + " đồng")
                .reduce((first, second) -> first + "; " + second).orElse("");
    }

    private String policyAnswer(String message) {
        if (containsAny(message, "doi tra", "doi hang", "hoan tien", "bao hanh", "chinh sach")) {
            return "Thông tin chính sách chưa được nạp vào chatbot local. Bạn vui lòng xem mục Chính sách trên website hoặc liên hệ cửa hàng để được xác nhận chính xác.";
        }
        return null;
    }

    private String productAnswer(String normalized) {
        if (!containsAny(normalized, "laptop", "may tinh", "dien thoai", "tai nghe",
                "mua", "tim", "tu van", "san pham", "gia")) return null;

        BigDecimal maxPrice = extractMaxPrice(normalized);
        String keyword = Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .reduce((first, second) -> first + " " + second).orElse("");
        List<String> tokens = Arrays.stream(keyword.split(" "))
                .filter(token -> token.length() > 1)
                .toList();
        List<Product> products = productService.findAllWithCategory().stream()
                .filter(product -> maxPrice == null || product.getPrice().compareTo(maxPrice) <= 0)
                .filter(product -> tokens.isEmpty() || tokens.stream().anyMatch(token -> searchable(product).contains(token)))
                .sorted(Comparator.comparingInt((Product product) -> score(product, tokens))
                        .reversed().thenComparing(Product::getId))
                .limit(5)
                .toList();
        if (products.isEmpty()) return "Mình chưa tìm thấy sản phẩm phù hợp trong dữ liệu cửa hàng.";
        return "Mình tìm thấy " + products.size() + " sản phẩm phù hợp: " + products.stream()
                .map(product -> product.getName() + " - " + product.getPrice()
                        + " đồng (/products/" + product.getId() + ")")
                .reduce((first, second) -> first + "; " + second).orElse("");
    }

    private String searchable(Product product) {
        return normalize(product.getName() + " " + product.getDescription() + " "
                + product.getCategory().getName() + " " + product.getCategory().getDescription());
    }

    private int score(Product product, List<String> tokens) {
        String name = normalize(product.getName());
        String category = normalize(product.getCategory().getName());
        return tokens.stream().mapToInt(token ->
                (name.contains(token) ? 4 : 0) + (category.contains(token) ? 2 : 0)).sum();
    }

    private BigDecimal extractMaxPrice(String message) {
        Matcher matcher = MILLION_PRICE.matcher(message);
        if (!matcher.find()) return null;
        return new BigDecimal(matcher.group(1).replace(',', '.')).multiply(BigDecimal.valueOf(1_000_000));
    }

    private boolean containsAny(String message, String... values) {
        return Arrays.stream(values).anyMatch(message::contains);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
