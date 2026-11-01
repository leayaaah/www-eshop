package iuh.fit.eshop.controller;
import iuh.fit.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController  {
    private final OrderService orders;
@GetMapping
public String mine(Authentication auth,Model model) {
        model.addAttribute("orders",orders.mine(auth.getName()));
        return "order/my-orders";
    }
@GetMapping("/{id}")
public String detail(@PathVariable Long id,Authentication auth,Model model) {
        var order=orders.findById(id);
        if(!orders.belongsTo(order,auth.getName())&&!auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_SELLER")))return "error/403";
        model.addAttribute("order",order);
        return "order/detail";
    }
}
