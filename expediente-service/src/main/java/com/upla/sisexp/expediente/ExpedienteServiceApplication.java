package com.upla.sisexp.expediente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ExpedienteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpedienteServiceApplication.class, args);
    }
}
