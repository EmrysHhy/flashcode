package com.bitejiuyeke.bitecommoncore.config;

import com.bitejiuyeke.bitecommoncore.domain.enums.RejectType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 */
@EnableAsync
@Configuration
public class ThreadPoolConfig {


    /**
     * 核心线程数
     */
    @Value("${thread.pool-executor.corePoolSize:5}")
    private Integer corePoolSize;

    /**
     * 最大线程数
     */
    @Value("${thread.pool-executor.maxPoolSize:100}")
    private Integer maxPoolSize;

    /**
     * 阻塞队列大小
     */
    @Value("${thread.pool-executor.queueCapacity:100}")
    private Integer queueCapacity;

    /**
     * 空闲存活时间
     */
    @Value("${thread.pool-executor.keepAliveSeconds:60}")
    private Integer keepAliveSeconds;

    /**
     * 线程名称
     */
    @Value("${thread.pool-executor.prefixName:thread-service-}")
    private String prefixName;

    /**
     * 拒绝策略
     */
    @Value("${thread.pool-executor.rejectHandler:2}")
    private Integer rejectHandler;


    /**
     * 注册和配置线程池执行器
     *
     * @return 线程池执行器
     */
    @Bean("threadPoolTaskExecutor")
    public Executor getThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(prefixName);
        //设置线程池关闭的时候 等待所有的任务完成后再继续销毁其他的bean
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //策略
        executor.setRejectedExecutionHandler(getRejectHandler());
        return executor;
    }

    /**
     * 拒绝策略
     *
     * @return 拒绝策略处理器
     */
    public RejectedExecutionHandler getRejectHandler() {
        if (RejectType.AbortPolicy.getValue().equals(rejectHandler)) {
            return new ThreadPoolExecutor.AbortPolicy();
        } else if (RejectType.CallerRunsPolicy.getValue().equals(rejectHandler)) {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        } else if (RejectType.DiscardOldestPolicy.getValue().equals(rejectHandler)) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        } else {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
    }
}
