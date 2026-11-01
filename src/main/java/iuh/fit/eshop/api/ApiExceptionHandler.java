package iuh.fit.eshop.api;
import iuh.fit.eshop.exception.NotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestControllerAdvice(basePackages="iuh.fit.eshop.api")
public class ApiExceptionHandler  {
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<?> notFound(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));
    }
@ExceptionHandler({IllegalStateException.class,IllegalArgumentException.class})
public ResponseEntity<?> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
    }
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        String msg=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).reduce((a,b)->a+"; "+b).orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of("error",msg));
    }
}
