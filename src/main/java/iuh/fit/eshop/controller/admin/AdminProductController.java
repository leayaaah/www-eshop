package iuh.fit.eshop.controller.admin;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController  {
    private final ProductService products;
    private final CategoryService categories;
@GetMapping
public String list(@RequestParam(required=false) String keyword,Model m) {
        m.addAttribute("products",products.search(keyword,null,0,100));
        m.addAttribute("keyword",keyword);
        return "admin/product/list";
    }
@GetMapping("/new")
public String create(Model m) {
        m.addAttribute("product",new Product());
        m.addAttribute("categories",categories.findAll());
        return "admin/product/form";
    }
@GetMapping("/{id}/edit")
public String edit(@PathVariable Long id,Model m) {
        m.addAttribute("product",products.findById(id));
        m.addAttribute("categories",categories.findAll());
        return "admin/product/form";
    }
@PostMapping
public String save(@Valid
@ModelAttribute Product product,BindingResult r,@RequestParam Long categoryId,Model m) {
        if(r.hasErrors()) {
            m.addAttribute("categories",categories.findAll());
            return "admin/product/form";
        }
        products.save(product,categoryId);
        return "redirect:/admin/products";
    }
@PostMapping("/{id}/delete")
public String delete(@PathVariable Long id) {
        products.delete(id);
        return "redirect:/admin/products";
    }
}
