package com.chathub.chathub.repository;

import com.chathub.chathub.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.logging.Logger;

public class UsersRepository {
    private static final Logger LOGGER = Logger.getLogger(UsersRepository.class.getName());

    private static final String USERNAME_HASH_KEY = "username";
    private static final String USERNAME_KEY = "username:%s";
    private static final String ID_HASH_KEY = "id";
    private static final String IS_ONLINE_HASH_KEY = "isOnline";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public User getUserById(int id) {
        String username = redisTemplate.opsForHash().get(String.format(USERNAME_KEY, id), USERNAME_HASH_KEY);
        boolean isOnline = Boolean.parseBoolean(redisTemplate.opsForHash().get(String.format(USERNAME_KEY, id), IS_ONLINE_HASH_KEY));
        return new User(id, username, isOnline);
    }
}
