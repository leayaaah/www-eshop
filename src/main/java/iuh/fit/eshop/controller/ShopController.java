package iuh.fit.eshop.controller;
import iuh.fit.eshop.service.*;
import iuh.fit.eshop.ai.RecommendationService;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequiredArgsConstructor
public class ShopController  {
    private final ProductService products;
    private final CategoryService categories;
    private final RecommendationService recommendations;
    private final iuh.fit.eshop.tracking.TrackingService tracking;
@GetMapping("/")
public String index(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,
                    @RequestParam(defaultValue="0") int page, Model model, HttpServletRequest request) {
        var result=products.search(keyword,categoryId,page,12);
        model.addAttribute("products",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("categoryId",categoryId);
        model.addAttribute("categories",categories.findAll());
        if (keyword == null && categoryId == null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var username = auth == null || !auth.isAuthenticated() ? null : auth.getName();
            String sessionId = username == null ? request.getSession(true).getId() : null;
            model.addAttribute("recommended", recommendations.recommend(username, sessionId, 8));
        }

        return "shop/index";
    }
@GetMapping("/products/{id}")
public String detail(@PathVariable Long id,Model model, HttpServletRequest request) {
        var product = products.findById(id);
       tracking.recordServer(request, id, iuh.fit.eshop.tracking.EventType.VIEW);
        model.addAttribute("product", product);
        model.addAttribute("similar", recommendations.similar(product, 8));
        return "shop/detail";
    }
}
