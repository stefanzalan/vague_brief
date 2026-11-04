package com.stefanzalan.vague_brief.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution using Java 21 Virtual Threads.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * Executor that creates a new virtual thread for each task.
     * Virtual threads are lightweight and suitable for I/O-bound operations
     * like sending notifications via email or Slack webhooks.
     *
     * @return a virtual thread per task executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    }
}
