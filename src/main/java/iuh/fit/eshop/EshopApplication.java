package iuh.fit.eshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class EshopApplication  {
    public static void main(String[] args)  {
        SpringApplication.run(EshopApplication.class, args);
    }
}