package com.chathub.chathub.config;

import com.chathub.chathub.model.User;
import com.chathub.chathub.service.MessageSubscriber;
import io.lettuce.core.SslOptions;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import javax.net.ssl.TrustManagerFactory;
import java.net.URI;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutos
public class RedisAppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisAppConfig.class);

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @Primary
    public StringRedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(new MessageSubscriber());
    }

    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory,
                                                 MessageListenerAdapter messageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListener(), topic());
        return container;
    }

    @Bean
    ChannelTopic topic() {
        return new ChannelTopic("MESSAGES");
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String redisUrl = System.getenv("REDISCLOUD_URL");

        if (redisUrl == null || redisUrl.isEmpty()) {
            throw new RuntimeException("REDISCLOUD_URL environment variable is not set");
        }
        //ler variaveis de ambiente
        URI redisUri = URI.create(redisUrl);
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(
                redisUri.getHost(),
                redisUri.getPort()
        );


        String[] userInfo = redisUri.getUserInfo().split(":");
        if (userInfo.length == 1) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(userInfo[0]));
        } else if (userInfo.length == 2) {
            redisStandaloneConfiguration.setUsername(userInfo[0]);
            redisStandaloneConfiguration.setPassword(RedisPassword.of(userInfo[1]));
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl().
                disablePeerVerification()
                .build();

        System.out.println("🔴 Redis Connection Attempt:");
        System.out.println("➡ Host: " + redisStandaloneConfiguration.getHostName());
        System.out.println("➡ Port: " + redisStandaloneConfiguration.getPort());
        System.out.println("➡ Username: " + redisStandaloneConfiguration.getUsername());
        System.out.println("➡ Password: " + redisStandaloneConfiguration.getPassword().toString());

        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);

    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplateObject(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, User> redisUserTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, User> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

}
