package com.merfonteen.profileservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ResilienceBeans {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService resilienceExecutor() {
        return Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    }
}
