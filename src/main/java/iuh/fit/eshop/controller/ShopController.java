package iuh.fit.eshop.controller;
import iuh.fit.eshop.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequiredArgsConstructor
public class ShopController  {
    private final ProductService products;
    private final CategoryService categories;
@GetMapping("/")
public String index(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="0") int page,Model model) {
        var result=products.search(keyword,categoryId,page,12);
        model.addAttribute("products",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("categoryId",categoryId);
        model.addAttribute("categories",categories.findAll());
        return "shop/index";
    }
@GetMapping("/products/{id}")
public String detail(@PathVariable Long id,Model model) {
        model.addAttribute("product",products.findById(id));
        return "shop/detail";
    }
}
