package iuh.fit.eshop.controller.admin;
import iuh.fit.eshop.dto.UserForm;
import iuh.fit.eshop.entity.User;
import iuh.fit.eshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController  {
    private final UserService users;
@GetMapping
public String list(Model m) {
        m.addAttribute("users",users.findAll());
        return "admin/user/list";
    }
@GetMapping("/new")
public String create(Model m) {
        m.addAttribute("form",new UserForm());
        return "admin/user/form";
    }
@PostMapping
public String save(@Valid
@ModelAttribute("form") UserForm f,BindingResult r,Model m) {
        if(r.hasErrors()||f.getPassword()==null||f.getPassword().length()<6) {
            m.addAttribute("error","Mật khẩu tối thiểu 6 ký tự");
            return "admin/user/form";
        }
        User u=new User();
        u.setUsername(f.getUsername());
        u.setFullName(f.getFullName());
        u.setEmail(f.getEmail());
        u.setPhone(f.getPhone());
        u.setAddress(f.getAddress());
        u.setRole(f.getRole());
        u.setEnabled(f.isEnabled());
        u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(f.getPassword()));
        users.save(u);
        return "redirect:/admin/users";
    }
@PostMapping("/{id}/delete")
public String delete(@PathVariable Long id,Authentication auth) {
        users.delete(id,auth.getName());
        return "redirect:/admin/users";
    }
}
