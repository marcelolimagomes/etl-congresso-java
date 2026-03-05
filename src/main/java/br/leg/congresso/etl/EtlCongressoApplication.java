package br.leg.congresso.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EtlCongressoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlCongressoApplication.class, args);
    }
}
