package com.thiago.sgai; // IMPORTANTE: Se as suas pastas físicas ainda forem com/example/demo, mude aqui para: package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- Importe aqui

@SpringBootApplication
@EnableScheduling // <-- Adicione esta anotação mágica aqui!
public class SgaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgaiApplication.class, args);
        System.out.println("\n=============================================");
        System.out.println("  SGAI BACKEND IS RUNNING AND READY TO GO!  ");
        System.out.println("=============================================\n");
    }
}