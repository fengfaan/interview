package com.interviewassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 统一的线程池配置，替代各 Controller 中散落的 Executors.newCachedThreadPool()。
 * 本地单用户场景下，核心线程 2 + 最大线程 5 足够应对 SSE 并发需求。
 * Spring 容器关闭时会自动调用 shutdown，确保资源释放。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("sse-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
