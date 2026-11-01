package iuh.fit.eshop.controller;
import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes  {
    private final Cart cart;
    private final CategoryService categories;
@ModelAttribute
public void common(Model model) {
        model.addAttribute("cartCount",cart.getCount());
        model.addAttribute("navCategories",categories.findAll());
    }
}
