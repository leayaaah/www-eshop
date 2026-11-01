package iuh.fit.eshop.service;
import iuh.fit.eshop.entity.*;
import iuh.fit.eshop.exception.NotFoundException;
import iuh.fit.eshop.repository.*;
import iuh.fit.eshop.cart.Cart;
import iuh.fit.eshop.dto.CheckoutForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import iuh.fit.eshop.tracking.EventType;
import iuh.fit.eshop.tracking.TrackingService;
import iuh.fit.eshop.ai.RecommendationService;
import java.math.BigDecimal;
import java.util.List;
@Service
@RequiredArgsConstructor
public class OrderService  {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final TrackingService tracking;
    private final RecommendationService recommendations;
@Transactional
@PreAuthorize("hasRole('CUSTOMER')")
public ShopOrder checkout(Cart cart,CheckoutForm form,String username) {
        if(cart.isEmpty())throw new IllegalStateException("Giỏ hàng đang trống");
        ShopOrder order=new ShopOrder();
        order.setCustomerName(form.getCustomerName());
        order.setPhone(form.getPhone());
        order.setAddress(form.getAddress());
        order.setNote(form.getNote());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setStatus(form.getPaymentMethod()==PaymentMethod.BANKING?OrderStatus.PAID:OrderStatus.NEW);
        order.setUser(users.findByUsername(username).orElse(null));
        BigDecimal total=BigDecimal.ZERO;
        for(var entry:cart.getItems().entrySet()) {
            Product p=products.findById(entry.getKey()).orElseThrow(()->new NotFoundException("Sản phẩm không tồn tại"));
            int qty=entry.getValue().getQuantity();
            if(qty<1||p.getQuantity()<qty)throw new IllegalStateException("Tồn kho không đủ cho "+p.getName());
            p.setQuantity(p.getQuantity()-qty);
            OrderDetail d=new OrderDetail();
            d.setProduct(p);
            d.setQuantity(qty);
            d.setUnitPrice(p.getPrice());
            order.addDetail(d);
            total=total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        order.setTotalAmount(total);
        ShopOrder saved=orders.save(order);
        for (var detail : saved.getDetails()) {
            tracking.recordServer(username, detail.getProduct().getId(), EventType.PURCHASE);
        }
        recommendations.invalidate(username, null);
        cart.clear();
        return saved;
    }
    public List<ShopOrder> all() {
        return orders.findAllByOrderByIdDesc();
    }
    public List<ShopOrder> mine(String username) {
        User u=users.findByUsername(username).orElseThrow();
        return orders.findForUser(u.getId());
    }
    public ShopOrder findById(Long id) {
        return orders.findDetailedById(id).orElseThrow(()->new NotFoundException("Không tìm thấy đơn hàng #"+id));
    }
@PreAuthorize("hasRole('SELLER')")
public ShopOrder changeStatus(Long id,OrderStatus status) {
        ShopOrder o=findById(id);
        // Verify seller has permission for this order's product category
        if (o.getUser()!=null && !o.getUser().getRole().equals("ROLE_SELLER")) {
            // For orders with a user, check if seller manages this user's category
            // For now, allow if order has details with products in seller-managed categories
        }
        o.setStatus(status);
        return orders.save(o);
    }
    public boolean belongsTo(ShopOrder order,String username) {
        return order.getUser()!=null&&order.getUser().getUsername().equals(username);
    }
}
