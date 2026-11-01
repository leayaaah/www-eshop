package iuh.fit.eshop.dto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class RegisterForm  {
@NotBlank
@Size(max=50)
private String username;
@NotBlank
@Size(min=6,max=72)
private String password;
@NotBlank
@Size(max=100)
private String fullName;
@NotBlank
@Email
@Size(max=100)
private String email;
@Size(max=20)
private String phone;
@Size(max=255)
private String address;
}
