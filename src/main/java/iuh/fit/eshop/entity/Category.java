package iuh.fit.eshop.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="category")
@Getter
@Setter
public class Category  {
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY)
private Long id;
@NotBlank
@Size(max=100)
@Column(nullable=false, unique=true, length=100)
private String name;
@Size(max=255)
private String description;
@OneToMany(mappedBy="category")
private List<Product> products = new ArrayList<>();
}
