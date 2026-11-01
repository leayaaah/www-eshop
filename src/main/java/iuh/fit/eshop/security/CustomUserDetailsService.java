package iuh.fit.eshop.security;
import iuh.fit.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService  {
    private final UserRepository users;
@Override
public UserDetails loadUserByUsername(String username)throws UsernameNotFoundException  {
        var u=users.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return User.withUsername(u.getUsername()).password(u.getPassword()).authorities(u.getRole()).disabled(!u.isEnabled()).build();
    }
}
