package com.chathub.chathub.service;

import com.chathub.chathub.util.RedisConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class UserService {
    //private Jedis jedis = RedisConnectionFactory.getJedisClient();

    private final Jedis jedis;
    @Autowired
    public UserService(Jedis jedis) {
        this.jedis = jedis;
    }
    public void updateUserStatus(String userId, boolean isOnline) {
        String status = isOnline ? "online" : "offline";
        jedis.publish("user-status", userId + ":" + status);
    }
}
