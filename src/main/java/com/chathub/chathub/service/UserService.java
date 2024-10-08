package com.chathub.chathub.service;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class UserService {
    private Jedis jedis = new Jedis("localhost", 6379);

    public void updateUserStatus(String userId, boolean isOnline) {
        String status = isOnline ? "online" : "offline";
        jedis.publish("user-status", userId + ":" + status);
    }
}
