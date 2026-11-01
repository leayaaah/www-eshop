package iuh.fit.eshop.controller.admin;
import iuh.fit.eshop.entity.OrderStatus;
import iuh.fit.eshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController  {
    private final OrderService orders;
@GetMapping
public String list(Model m) {
        m.addAttribute("orders",orders.all());
        return "admin/order/list";
    }
@GetMapping("/{id}")
public String detail(@PathVariable Long id,Model m) {
        m.addAttribute("order",orders.findById(id));
        m.addAttribute("statuses",OrderStatus.values());
        return "admin/order/detail";
    }
@PostMapping("/{id}/status")
public String status(@PathVariable Long id,@RequestParam OrderStatus status) {
        orders.changeStatus(id,status);
        return "redirect:/admin/orders/"+id;
    }
}
