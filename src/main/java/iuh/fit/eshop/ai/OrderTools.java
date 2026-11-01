package iuh.fit.eshop.ai;

import iuh.fit.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTools {
    private final OrderService orderService;

    public record OrderInfo(Long id, String status, BigDecimal total) {}

    @Tool(description = "Liệt kê các đơn hàng của khách đang đăng nhập")
    public List<OrderInfo> myOrders(@ToolParam(description = "Tên người dùng") String username) {
        return orderService.mine(username).stream()
                .map(o -> new OrderInfo(o.getId(), o.getStatus().name(), o.getTotalAmount()))
                .toList();
    }
}