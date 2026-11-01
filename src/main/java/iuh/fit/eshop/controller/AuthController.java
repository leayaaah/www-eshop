package iuh.fit.eshop.controller;
import iuh.fit.eshop.dto.RegisterForm;
import iuh.fit.eshop.entity.User;
import iuh.fit.eshop.security.JwtService;
import iuh.fit.eshop.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
@RequiredArgsConstructor
public class AuthController  {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService users;
@GetMapping("/login")
public String login() {
        return "auth/login";
    }
@PostMapping("/login")
public String loginSubmit(@RequestParam String username,@RequestParam String password,HttpServletResponse response,Model model) {
        try {
            var auth=authManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));
            String token=jwt.generateToken((UserDetails)auth.getPrincipal());
            response.addHeader("Set-Cookie",ResponseCookie.from(JwtService.COOKIE_NAME,token).httpOnly(true).secure(true).path("/").sameSite("Lax").maxAge(jwt.getExpirationSeconds()).build().toString());
            boolean seller=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_SELLER"));
            return "redirect:"+(seller?"/admin/products":"/");
        } catch(AuthenticationException ex) {
            model.addAttribute("error","Sai tài khoản, mật khẩu hoặc tài khoản đã bị khoá");
            return "auth/login";
        }
    }
@GetMapping("/register")
public String register(Model model) {
        model.addAttribute("form",new RegisterForm());
        return "auth/register";
    }
@PostMapping("/register")
public String registerSubmit(@Valid
@ModelAttribute("form") RegisterForm form,BindingResult result,Model model) {
        if(result.hasErrors())return "auth/register";
        try {
            User u=new User();
            u.setUsername(form.getUsername());
            u.setFullName(form.getFullName());
            u.setEmail(form.getEmail());
            u.setPhone(form.getPhone());
            u.setAddress(form.getAddress());
            users.register(u,form.getPassword());
            return "redirect:/login?registered";
        } catch(IllegalArgumentException e) {
            model.addAttribute("error",e.getMessage());
            return "auth/register";
        }
    }
@GetMapping("/403")
public String forbidden() {
        return "error/403";
    }
}
