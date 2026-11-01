package iuh.fit.eshop.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter  {
    private final JwtService jwt;
    private final UserDetailsService users;
@Override
protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException  {
        String token=null;
        String auth=req.getHeader("Authorization");
        if(auth!=null&&auth.startsWith("Bearer "))token=auth.substring(7);
        if(token==null&&req.getCookies()!=null)for(Cookie c:req.getCookies())if(JwtService.COOKIE_NAME.equals(c.getName()))token=c.getValue();
        if(token!=null&&SecurityContextHolder.getContext().getAuthentication()==null) {
            try {
                String username=jwt.username(token);
                UserDetails user=users.loadUserByUsername(username);
                if(user.isEnabled()&&jwt.valid(token,user)) {
                    var authentication=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch(RuntimeException ignored) {
            }
        }
        chain.doFilter(req,res);
    }
}
