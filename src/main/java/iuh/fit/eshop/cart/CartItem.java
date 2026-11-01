package iuh.fit.eshop.cart;
import iuh.fit.eshop.entity.Product;
import lombok.Getter;
@Getter
public class CartItem  {
    private final Product product;
    private int quantity;
    public CartItem(Product product,int quantity) {
        this.product=product;
        this.quantity=quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity=quantity;
    }
    public void increase(int n) {
        quantity+=n;
    }
}
