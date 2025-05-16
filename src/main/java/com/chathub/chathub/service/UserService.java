package com.chathub.chathub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class UserService {

    private final Jedis jedisPub;

    @Autowired
    public UserService(@Qualifier("jedisPub") Jedis jedisPub) {
        this.jedisPub = jedisPub;
    }
    public void updateUserStatus(String userId, boolean isOnline) {
        String status = isOnline ? "online" : "offline";
        jedisPub.publish("user-status", userId + ":" + status);
    }
}
