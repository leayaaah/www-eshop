package iuh.fit.eshop.controller.admin;
import iuh.fit.eshop.entity.Category;
import iuh.fit.eshop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController  {
    private final CategoryService service;
@GetMapping
public String list(Model m) {
        m.addAttribute("categories",service.findAll());
        return "admin/category/list";
    }
@GetMapping("/new")
public String create(Model m) {
        m.addAttribute("category",new Category());
        return "admin/category/form";
    }
@GetMapping("/{id}/edit")
public String edit(@PathVariable Long id,Model m) {
        m.addAttribute("category",service.findById(id));
        return "admin/category/form";
    }
@PostMapping
public String save(@Valid
@ModelAttribute Category category,BindingResult r) {
        if(r.hasErrors())return "admin/category/form";
        service.save(category);
        return "redirect:/admin/categories";
    }
@PostMapping("/{id}/delete")
public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/admin/categories";
    }
}
