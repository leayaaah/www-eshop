package iuh.fit.eshop.service;
import iuh.fit.eshop.entity.User;
import iuh.fit.eshop.exception.NotFoundException;
import iuh.fit.eshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class UserService  {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    public List<User> findAll() {
        return repo.findAll();
    }
    public User findById(Long id) {
        return repo.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy tài khoản #"+id));
    }
    public User findByUsername(String username) {
        return repo.findByUsername(username).orElseThrow(()->new NotFoundException("Không tìm thấy tài khoản"));
    }
@PreAuthorize("hasRole('SELLER')")
public User save(User u) {
        return repo.save(u);
    }
@Transactional
public User register(User u,String rawPassword) {
        if(repo.existsByUsername(u.getUsername()))throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        if(repo.existsByEmail(u.getEmail()))throw new IllegalArgumentException("Email đã tồn tại");
        u.setPassword(encoder.encode(rawPassword));
        u.setRole("ROLE_CUSTOMER");
        u.setEnabled(true);
        return repo.save(u);
    }
@PreAuthorize("hasRole('SELLER')")
@Transactional
public void delete(Long id,String currentUsername) {
        User u=findById(id);
        if(u.getUsername().equals(currentUsername))throw new IllegalStateException("Không thể tự xoá tài khoản đang đăng nhập");
        repo.delete(u);
    }
}
