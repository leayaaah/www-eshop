package iuh.fit.eshop.controller;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class ErrorPageController implements ErrorController  {
@RequestMapping("/error")
public String error(HttpServletRequest request, Model model)  {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        model.addAttribute("status", status == null ? "Lỗi" : status.toString());
        model.addAttribute("error", message == null || message.toString().isBlank()
        ? "Đã xảy ra lỗi khi xử lý yêu cầu. Hãy kiểm tra log ứng dụng để biết nguyên nhân chi tiết."
        : message.toString());
        return "error/general";
    }
}
