package com.ntros.mprocswift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ntros.mprocswift")
public class MoneyProcessorSwiftServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyProcessorSwiftServerApplication.class, args);
    }

}
