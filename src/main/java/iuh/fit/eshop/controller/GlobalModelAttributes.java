package iuh.fit.eshop.controller;
import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.beans.factory.annotation.Value;
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes  {
    private final Cart cart;
    private final CategoryService categories;
    @Value("${app.tracking.client-enabled:true}")
    private boolean clientTrackingEnabled;
@ModelAttribute
public void common(Model model) {
        model.addAttribute("cartCount",cart.getCount());
        model.addAttribute("navCategories",categories.findAll());
        model.addAttribute("clientTrackingEnabled", clientTrackingEnabled);
    }
}
