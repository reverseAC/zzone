package com.zjh.zzone.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局线程池配置
 *
 * @author zjh
 * @date 2025/7/28 22:33
 */
@Configuration
@EnableAsync(proxyTargetClass = true)
public class ThreadPoolConfig {

    /**
     * CPU核数
     */
    public static final int cpuNum = Runtime.getRuntime().availableProcessors();
    /**
     * 核心线程数（默认线程数）
     */
    private static final int corePoolSize = Math.max(cpuNum * 2, 16);
    /**
     * 最大线程数
     */
    private static final int maxPoolSize = corePoolSize * 10;
    /**
     * 允许线程空闲时间（单位：默认为秒）
     *      针对大于corePoolSize部分的线程的空闲生存时间
     */
    private static final int keepAliveTime = 30;
    /**
     * 缓冲队列大小
     */
    private static final int queueCapacity = 10000;
    /**
     * 线程池名前缀
     */
    private static final String threadNamePrefix = "async-";


    @Bean("taskExecutor") // bean的名称，默认为首字母小写的方法名
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveTime);
        executor.setThreadNamePrefix(threadNamePrefix);

        // 线程池对拒绝任务的处理策略
        // CallerRunsPolicy：由调用线程（提交任务的线程）处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化
        executor.initialize();
        return executor;
    }
}
