package com.upla.sisexp.presupuesto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PresupuestoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PresupuestoServiceApplication.class, args);
    }
}
