package iuh.fit.eshop.api;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController  {
    private final ProductService service;
    public record ProductDto(Long id,String name,String description,BigDecimal price,Integer quantity,String imageUrl,Long categoryId,String categoryName) {
        static ProductDto from(Product p) {
            return new ProductDto(p.getId(),p.getName(),p.getDescription(),p.getPrice(),p.getQuantity(),p.getImageUrl(),p.getCategory().getId(),p.getCategory().getName());
        }
    }
    public record ProductRequest(@NotBlank
@Size(max=150) String name,String description,@NotNull
@DecimalMin("0.00") BigDecimal price,@NotNull
@Min(0) Integer quantity,String imageUrl,@NotNull Long categoryId) {
    }
@GetMapping
public Page<ProductDto> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return service.search(keyword,categoryId,page,size).map(ProductDto::from);
    }
@GetMapping("/{id}")
public ProductDto get(@PathVariable Long id) {
        return ProductDto.from(service.findById(id));
    }
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasAuthority('ROLE_SELLER')")
public ProductDto create(@Valid
@RequestBody ProductRequest r) {
        return ProductDto.from(service.save(apply(r,new Product()),r.categoryId()));
    }
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('ROLE_SELLER')")
public ProductDto update(@PathVariable Long id,@Valid
@RequestBody ProductRequest r) {
        return ProductDto.from(service.save(apply(r,service.findById(id)),r.categoryId()));
    }
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@PreAuthorize("hasAuthority('ROLE_SELLER')")
public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    private Product apply(ProductRequest r,Product p) {
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setQuantity(r.quantity());
        p.setImageUrl(r.imageUrl());
        return p;
    }
}
