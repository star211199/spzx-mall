package com.spzx.channel.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池初始化
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor init() {

        int cpuCount = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                cpuCount + 1,
                cpuCount * 2,
                0,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(100),
                Executors.defaultThreadFactory(),
                (runnable, executor) -> {
                    System.out.println(" 线程池达到了最大饱和... ");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    //再次将拒绝任务提交给线程池执行
                    executor.submit(runnable);
                }
        );
        //threadPoolExecutor.prestartAllCoreThreads(); //初始化所有核心线程数
        threadPoolExecutor.prestartCoreThread(); //初始化1个核心线程数

        //threadPoolExecutor.shutdown();
        return threadPoolExecutor;
    }

}
