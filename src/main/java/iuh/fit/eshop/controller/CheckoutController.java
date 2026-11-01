package iuh.fit.eshop.controller;
import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.dto.CheckoutForm;
import iuh.fit.eshop.entity.PaymentMethod;
import iuh.fit.eshop.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController  {
    private final Cart cart;
    private final OrderService orders;
    private final UserService users;
@GetMapping
public String form(Model model,Authentication auth) {
        if(cart.isEmpty())return "redirect:/cart";
        var f=new CheckoutForm();
        var u=users.findByUsername(auth.getName());
        f.setCustomerName(u.getFullName());
        f.setPhone(u.getPhone());
        f.setAddress(u.getAddress());
        model.addAttribute("form",f);
        model.addAttribute("paymentMethods",PaymentMethod.values());
        model.addAttribute("cart",cart);
        return "checkout/form";
    }
@PostMapping
public String submit(@Valid
@ModelAttribute("form") CheckoutForm form,BindingResult result,Authentication auth,Model model) {
        if(cart.isEmpty())return "redirect:/cart";
        if(result.hasErrors()) {
            model.addAttribute("paymentMethods",PaymentMethod.values());
            model.addAttribute("cart",cart);
            return "checkout/form";
        }
        var order=orders.checkout(cart,form,auth.getName());
        return "redirect:/orders/"+order.getId();
    }
}
