package iuh.fit.eshop.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig  {
    private final JwtService jwt;
    private final CustomUserDetailsService users;
@Bean PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
@Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c)throws Exception {
        return c.getAuthenticationManager();
    }
@Bean
@Order(1) SecurityFilterChain api(HttpSecurity http)throws Exception  {
        http.securityMatcher("/api/**").csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/api/auth/login").permitAll().requestMatchers(org.springframework.http.HttpMethod.GET,"/api/products","/api/products/**").permitAll().requestMatchers(org.springframework.http.HttpMethod.POST,"/api/products/**","/api/products").hasAuthority("ROLE_SELLER").requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/products/**").hasAuthority("ROLE_SELLER").requestMatchers(org.springframework.http.HttpMethod.DELETE,"/api/products/**").hasAuthority("ROLE_SELLER").requestMatchers(org.springframework.http.HttpMethod.POST,"/api/track").permitAll().anyRequest().authenticated()).exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401)).accessDeniedHandler((req,res,ex)->res.sendError(403))).addFilterBefore(new JwtAuthenticationFilter(jwt,users),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
@Bean
@Order(2) SecurityFilterChain web(HttpSecurity http)throws Exception  {
        http.authorizeHttpRequests(a->a.requestMatchers("/register","/login").anonymous().requestMatchers("/","/products/**","/css/**","/images/**","/error").permitAll().requestMatchers("/admin/**").hasAuthority("ROLE_SELLER").requestMatchers("/cart/**","/checkout/**","/orders/**").hasAuthority("ROLE_CUSTOMER").anyRequest().permitAll()).formLogin(f->f.loginPage("/login").disable()).logout(l->l.logoutUrl("/logout").logoutSuccessHandler((req,res,auth)->{if(req.getSession(false)!=null)req.getSession(false).invalidate();res.addHeader("Set-Cookie",JwtService.COOKIE_NAME+"=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");res.sendRedirect("/");})).exceptionHandling(e->e.accessDeniedPage("/403")).addFilterBefore(new JwtAuthenticationFilter(jwt,users),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
