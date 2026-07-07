package com.upla.sisexp.expediente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.upla.sisexp")
@EnableFeignClients
public class ExpedienteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpedienteServiceApplication.class, args);
    }
}
