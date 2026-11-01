package iuh.fit.eshop.api;
import iuh.fit.eshop.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController  {
    private final AuthenticationManager manager;
    private final JwtService jwt;
    public record LoginRequest(String username,String password) {
    }
    public record TokenResponse(String accessToken,String tokenType,long expiresIn) {
    }
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var a=manager.authenticate(new UsernamePasswordAuthenticationToken(req.username(),req.password()));
            return ResponseEntity.ok(new TokenResponse(jwt.generateToken((UserDetails)a.getPrincipal()),"Bearer",jwt.getExpirationSeconds()));
        } catch(AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error","Sai tài khoản, mật khẩu hoặc tài khoản bị khoá"));
        }
    }
}
