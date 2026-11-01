package iuh.fit.eshop.controller;
import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.service.ProductService;
import iuh.fit.eshop.tracking.EventType;
import iuh.fit.eshop.tracking.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController  {
    private final Cart cart;
    private final ProductService products;
    private final TrackingService tracking;
@GetMapping
public String view(Model model)  {
        model.addAttribute("cart", cart);
        return "cart/view";
    }
@PostMapping("/add/{id}")
public String add(@PathVariable Long id,
@RequestParam(defaultValue = "1") int quantity, HttpServletRequest request)  {
        var product = products.findById(id);
        int requested = Math.max(1, quantity);
        var existing = cart.getItems().get(id);
        int current = existing == null ? 0 : existing.getQuantity();
        int totalAfterAdd = requested + current;
        if (totalAfterAdd > product.getQuantity())  {
            throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho (còn " + product.getQuantity() + ")");
        }
        cart.add(product, requested);
        tracking.recordServer(request, id, EventType.ADD_TO_CART);
        return "redirect:/cart";
    }
@PostMapping("/update/{id}")
public String update(@PathVariable Long id,
@RequestParam int quantity)  {
        if (quantity > 0)  {
            var item = cart.getItems().get(id);
            if (item != null)  {
                var product = products.findById(id);
                if (quantity > product.getQuantity())  {
                    throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho");
                }
            }
        }
        cart.update(id, quantity);
        return "redirect:/cart";
    }
@PostMapping("/remove/{id}")
public String remove(@PathVariable Long id)  {
        cart.remove(id);
        return "redirect:/cart";
    }
}
