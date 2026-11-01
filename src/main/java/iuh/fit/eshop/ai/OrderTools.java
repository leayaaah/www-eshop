package iuh.fit.eshop.ai;

import iuh.fit.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTools {
    private final OrderService orderService;
    public record OrderInfo(Long id, String status, BigDecimal total) {}

    @Tool(description = "Liệt kê các đơn hàng của khách đang đăng nhập")
    public Object getMyRecentOrders() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return "Bạn cần đăng nhập.";
        return orderService.mine(authentication.getName()).stream()
                .map(o -> new OrderInfo(o.getId(), o.getStatus().name(), o.getTotalAmount()))
                .toList();
    }
}
