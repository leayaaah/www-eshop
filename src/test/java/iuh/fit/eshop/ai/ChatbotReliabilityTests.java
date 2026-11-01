package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.OrderService;
import iuh.fit.eshop.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatbotReliabilityTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        ToolCallBudget.clear();
    }

    @Test
    void capsToolCallsAtTwoPerRequest() {
        ToolCallBudget.start();
        ToolCallBudget.consume("searchProducts");
        ToolCallBudget.consume("getProductDetail");

        assertThrows(ToolCallBudget.ToolCallLimitExceededException.class,
                () -> ToolCallBudget.consume("getRecommendedProducts"));
    }

    @Test
    void unifiedProductSearchUsesVectorOnceAndDoesNotExposeSimilarTool() {
        ProductVectorSearchService vector = mock(ProductVectorSearchService.class);
        ProductService products = mock(ProductService.class);
        RecommendationService recommendations = mock(RecommendationService.class);
        Product product = product(1L);
        when(vector.search(anyString(), eq(8))).thenReturn(List.of(product));
        ProductTools tools = new ProductTools(products, vector, recommendations);

        List<ProductTools.ProductInfo> result = tools.searchProducts(
                "laptop học tập nhẹ pin tốt", "Laptop", BigDecimal.valueOf(20_000_000), true);

        assertEquals(1, result.size());
        verify(vector).search("laptop học tập nhẹ pin tốt", 8);
        verifyNoMoreInteractions(vector);
        assertTrue(java.util.Arrays.stream(ProductTools.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("findSimilarProducts"))
                .noneMatch(method -> method.isAnnotationPresent(Tool.class)));
    }

    @Test
    void vectorSearchUsesVectorStoreAndReturnsAtMostRequestedResults() {
        VectorStore store = mock(VectorStore.class);
        ObjectProvider<VectorStore> stores = provider(store);
        ProductService products = mock(ProductService.class);
        Product product = product(7L);
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("laptop", Map.of("source", "product", "productId", 7L))));
        when(products.findById(7L)).thenReturn(product);
        ProductVectorSearchService service = new ProductVectorSearchService(stores, products);
        ReflectionTestUtils.setField(service, "timeoutMillis", 1000L);

        assertEquals(List.of(product), service.search("laptop học tập", 5));
        verify(store).similaritySearch(any(SearchRequest.class));
        verify(products, never()).search(any(), any(), anyInt(), anyInt());
    }

    @Test
    void unauthenticatedOrderToolDoesNotReadAnotherUsersOrders() {
        OrderService orders = mock(OrderService.class);
        OrderTools tools = new OrderTools(orders);

        assertEquals("Bạn cần đăng nhập.", tools.getMyRecentOrders());
        verifyNoInteractions(orders);
    }

    @Test
    void geminiTimeoutReturnsProductFallback() throws Exception {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(client.prompt()).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.tools(any(Object[].class))).thenReturn(request);
        when(request.advisors(any(Consumer.class))).thenReturn(request);
        when(request.call()).thenAnswer(invocation -> {
            TimeUnit.SECONDS.sleep(2);
            return response;
        });

        ObjectProvider<ChatClient> clients = provider(client);
        ObjectProvider<ChatMemory> memories = provider(mock(ChatMemory.class));
        ProductVectorSearchService vector = mock(ProductVectorSearchService.class);
        ProductService products = mock(ProductService.class);
        when(vector.search(anyString(), eq(5))).thenReturn(List.of(product(9L)));
        ChatbotService service = new ChatbotService(clients, memories, mock(ProductTools.class),
                mock(OrderTools.class), vector, products);
        ReflectionTestUtils.setField(service, "chatTimeoutMillis", 50L);

        String message = "Tôi cần laptop học tập và lập trình, RAM từ 16GB, nhẹ, pin tốt, ngân sách tối đa 20 triệu đồng, ưu tiên máy bền và chạy mượt";
        String answer = service.chat(message, "conversation-1");

        assertTrue(answer.contains("/products/9"));
        verify(vector).search(message, 5);
    }

    @Test
    void longProductAdviceUsesOneLocalVectorSearchBeforeGemini() {
        ChatClient client = mock(ChatClient.class);
        ProductVectorSearchService vector = mock(ProductVectorSearchService.class);
        ProductService products = mock(ProductService.class);
        Product laptop = product(11L);
        when(vector.search(anyString(), eq(8))).thenReturn(List.of(laptop));
        ChatbotService service = new ChatbotService(provider(client), provider(mock(ChatMemory.class)),
                mock(ProductTools.class), mock(OrderTools.class), vector, products);

        String answer = service.chat(
                "Tôi đang cần tư vấn một chiếc laptop học tập và lập trình, ưu tiên nhẹ, pin tốt, RAM từ 16GB, ngân sách tối đa khoảng 20 triệu đồng",
                "conversation-1");

        assertTrue(answer.contains("/products/11"));
        verify(vector).search(anyString(), eq(8));
        verifyNoInteractions(client);
    }

    private static Product product(Long id) {
        Category category = new Category();
        category.setName("Laptop");
        Product product = new Product();
        product.setId(id);
        product.setName("Laptop học tập");
        product.setDescription("Nhẹ, pin tốt");
        product.setPrice(BigDecimal.valueOf(15_000_000));
        product.setQuantity(5);
        product.setCategory(category);
        return product;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
