package com.pavedroad.templateservice.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync(proxyTargetClass = true)
class AsyncConfig {
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 2
            queueCapacity = 500
            setThreadNamePrefix("AsyncExecutor-")
            initialize()
        }
    }
}