package com.chathub.chathub.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

import java.net.URI;

@Configuration
public class RedisJedisConfig {

    @Value("${redis.url}")
    private String REDIS_URI;

    @Bean(name = "jedisSub")
    public Jedis jedisSub() {
        return getJedisClient(REDIS_URI);
    }

    @Bean(name = "jedisPub")
    public Jedis jedisPub() {
        return getJedisClient(REDIS_URI);
    }

    public Jedis getJedisClient(String REDIS_URI) {
        URI redisUri = URI.create(REDIS_URI);
        String[] userInfo = redisUri.getUserInfo().split(":");
        String username = userInfo[0];
        String password = userInfo[1];

        HostAndPort hostAndPort = new HostAndPort(redisUri.getHost(), redisUri.getPort());

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(false)
                .user(username)
                .password(password)
                .build();

        return new Jedis(hostAndPort, clientConfig);
    }
}
