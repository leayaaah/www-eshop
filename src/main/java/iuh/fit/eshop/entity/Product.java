package iuh.fit.eshop.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name="product")
@Getter
@Setter
public class Product  {
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY)
private Long id;
@NotBlank
@Size(max=150)
@Column(nullable=false,length=150)
private String name;
@Size(max=500)
@Column(length=500)
private String description;
@NotNull
@DecimalMin("0.00")
@Column(nullable=false,precision=15,scale=2)
private BigDecimal price;
@NotNull
@Min(0)
@Column(nullable=false)
private Integer quantity=0;
@Size(max=500)
@Column(name="image_url",length=500)
private String imageUrl;
@ManyToOne(fetch=FetchType.LAZY,optional=false)
@JoinColumn(name="category_id",nullable=false)
private Category category;
@Column(name="created_at",insertable=false,updatable=false)
private LocalDateTime createdAt;
}
