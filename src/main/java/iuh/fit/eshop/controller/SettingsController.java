package iuh.fit.eshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {
    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
