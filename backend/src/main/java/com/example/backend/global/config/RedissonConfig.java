package com.example.backend.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${app.redis.address:redis://localhost:6379}") String address) {
        Config config = new Config();
        config.setLockWatchdogTimeout(30000);
        config.useSingleServer().setAddress(address)
                .setConnectTimeout(2000).setTimeout(2000).setRetryAttempts(0);
        return Redisson.create(config);
    }
}
