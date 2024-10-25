package com.supply.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress("redis://192.168.65.150:6379", "redis://192.168.65.150:6380",
                        "redis://192.168.65.150:6381", "redis://192.168.65.150:6382",
                        "redis://192.168.65.150:6383", "redis://192.168.65.150:6384");
        return Redisson.create(config);
    }

}
