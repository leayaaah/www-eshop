package iuh.fit.eshop.ai;

import iuh.fit.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OrderTools {
    private final OrderService orderService;

    public record OrderInfo(Long id, String status, BigDecimal total) {}

    @Tool(description = "Liệt kê các đơn hàng của khách đang đăng nhập")
    public List<OrderInfo> myOrders() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }
        String username = authentication.getName();
        return orderService.mine(username).stream()
                .map(o -> new OrderInfo(o.getId(), o.getStatus().name(), o.getTotalAmount()))
                .toList();
    }
}