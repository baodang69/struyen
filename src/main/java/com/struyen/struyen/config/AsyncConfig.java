package com.struyen.struyen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Cấu hình ThreadPoolTaskExecutor để kiểm soát số lượng luồng chạy ngầm (@Async),
     * tránh việc Spring khởi tạo vô hạn thread (SimpleAsyncTaskExecutor) gây cạn kiệt CPU/RAM.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("struyen-async-");
        executor.initialize();
        return executor;
    }
}

