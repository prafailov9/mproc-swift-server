package com.ntros.mprocswift.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public ExecutorService taskExecutor() {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(numberOfThreads);
    }


}
