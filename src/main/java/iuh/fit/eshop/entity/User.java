package iuh.fit.eshop.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name="users")
@Getter
@Setter
public class User  {
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY)
private Long id;
@NotBlank
@Size(max=50)
@Column(nullable=false,unique=true,length=50)
private String username;
@Column(nullable=false,length=100)
private String password;
@NotBlank
@Size(max=100)
@Column(name="full_name",nullable=false,length=100)
private String fullName;
@NotBlank
@Email
@Size(max=100)
@Column(nullable=false,unique=true,length=100)
private String email;
@Size(max=20)
private String phone;
@Size(max=255)
private String address;
@Column(nullable=false,length=30)
private String role="ROLE_CUSTOMER";
@Column(nullable=false)
private boolean enabled=true;
@Column(name="created_at",insertable=false,updatable=false)
private LocalDateTime createdAt;
}
