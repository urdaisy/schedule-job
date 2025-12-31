package com.schedule.job.admin.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private Integer redisDatabase;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient createRedissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%s", redisHost, redisPort);
        config.useSingleServer().setAddress(address)
                .setDatabase(this.getRedisDatabase() != null ? this.getRedisDatabase() : 0);
        if (this.getRedisPassword() != null && !this.getRedisPassword().isEmpty()) {
            config.useSingleServer().setPassword(this.getRedisPassword());
        }
        return Redisson.create(config);
    }
}
