package com.gzx.gzxpicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
@Configuration
@ConfigurationProperties("thread-pool")
public class ThreadPoolConfig {
    private int corePoolSize;
    private int maxPoolSize;
    private int keepAliveTime;
    private int queueCapacity;
    @Bean("customExecutor")
    public ThreadPoolExecutor customExecutor() {
        return new  ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),new ThreadPoolExecutor.DiscardPolicy());
}

}
