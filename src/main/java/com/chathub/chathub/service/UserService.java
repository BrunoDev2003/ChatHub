package com.chathub.chathub.service;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.net.URI;

@Service
public class UserService {
    URI redisUri = URI.create("rediss://default:wIVkh3YXQbSKBuaPlUQudER6IhvtnQUU@redis-16592.c61.us-east-1-3.ec2.redns.redis-cloud.com:16592");
    private Jedis jedis = new Jedis(redisUri);

    public void updateUserStatus(String userId, boolean isOnline) {
        String status = isOnline ? "online" : "offline";
        jedis.publish("user-status", userId + ":" + status);
    }
}
