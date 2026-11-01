package iuh.fit.eshop.cart;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
@Component
@SessionScope
@Getter
public class Cart  {
    private final Map<Long,CartItem> items=new LinkedHashMap<>();
    public void add(iuh.fit.eshop.entity.Product p,int quantity) {
        if(quantity<1)throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        CartItem i=items.get(p.getId());
        if(i==null)items.put(p.getId(),new CartItem(p,quantity));
        else i.increase(quantity);
    }
    public void update(Long id,int quantity) {
        if(quantity<=0)items.remove(id);
        else if(items.containsKey(id))items.get(id).setQuantity(quantity);
    }
    public void remove(Long id) {
        items.remove(id);
    }
    public void clear() {
        items.clear();
    }
    public boolean isEmpty() {
        return items.isEmpty();
    }
    public int getCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }
    public BigDecimal getTotal() {
        return items.values().stream().map(i->i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
}
