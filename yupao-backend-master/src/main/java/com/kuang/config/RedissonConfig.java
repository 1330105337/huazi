package com.kuang.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        // 创建配置
        Config config = new Config();
        String redisAdress=String.format("redis://%s:%s",host,port);
        //setdatebase是指redis库号
        config.useSingleServer().setAddress(redisAdress).setDatabase(3);
        //创建实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
