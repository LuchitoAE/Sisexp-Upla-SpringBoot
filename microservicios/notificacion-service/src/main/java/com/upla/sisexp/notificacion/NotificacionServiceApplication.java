package com.upla.sisexp.notificacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.upla.sisexp")
public class NotificacionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificacionServiceApplication.class, args);
    }
}
