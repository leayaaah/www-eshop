package iuh.fit.eshop.dto;
import iuh.fit.eshop.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class CheckoutForm  {
@NotBlank
@Size(max=100)
private String customerName;
@NotBlank
@Size(max=20)
private String phone;
@NotBlank
@Size(max=255)
private String address;
@Size(max=500)
private String note;
@NotNull
private PaymentMethod paymentMethod=PaymentMethod.COD;
}
