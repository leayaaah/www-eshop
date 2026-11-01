package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.OrderService;
import iuh.fit.eshop.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ChatbotReliabilityTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productAdviceUsesSqlWithoutVectorOrGemini() {
        ProductService products = mock(ProductService.class);
        Product laptop = product(1L);
        when(products.findAllWithCategory()).thenReturn(List.of(laptop));
        ChatbotService chatbot = new ChatbotService(products, mock(OrderService.class));

        String answer = chatbot.chat(
                "Tôi cần laptop học tập, RAM từ 16GB, ngân sách tối đa 20 triệu",
                "conversation-1");

        assertTrue(answer.contains("/products/1"));
        verify(products).findAllWithCategory();
    }

    @Test
    void policyQuestionReturnsExplicitLocalModeMessage() {
        ChatbotService chatbot = new ChatbotService(mock(ProductService.class), mock(OrderService.class));

        assertTrue(chatbot.chat("Cửa hàng có chính sách đổi trả không?", "conversation-1")
                .contains("chưa được nạp"));
    }

    @Test
    void unauthenticatedOrderQuestionDoesNotReadOrders() {
        OrderService orders = mock(OrderService.class);
        ChatbotService chatbot = new ChatbotService(mock(ProductService.class), orders);

        assertTrue(chatbot.chat("Hãy xem đơn hàng gần đây của tôi", "conversation-1")
                .contains("cần đăng nhập"));
        verifyNoInteractions(orders);
    }

    @Test
    void authenticatedOrderQuestionReadsOnlyCurrentUserOrders() {
        OrderService orders = mock(OrderService.class);
        when(orders.mine("alice")).thenReturn(List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "password", List.of()));
        ChatbotService chatbot = new ChatbotService(mock(ProductService.class), orders);

        assertTrue(chatbot.chat("Trạng thái đơn hàng của tôi?", "conversation-1")
                .contains("chưa có đơn"));
        verify(orders).mine("alice");
    }

    @Test
    void streamReturnsLocalAnswerImmediately() {
        ProductService products = mock(ProductService.class);
        when(products.findAllWithCategory()).thenReturn(List.of(product(2L)));
        ChatbotService chatbot = new ChatbotService(products, mock(OrderService.class));

        assertTrue(chatbot.stream("Tìm laptop", "conversation-1")
                .blockFirst().contains("/products/2"));
    }

    private static Product product(Long id) {
        Category category = new Category();
        category.setName("Laptop");
        Product product = new Product();
        product.setId(id);
        product.setName("Laptop học tập");
        product.setDescription("Nhẹ, pin tốt, RAM 16GB");
        product.setPrice(BigDecimal.valueOf(15_000_000));
        product.setQuantity(5);
        product.setCategory(category);
        return product;
    }
}
