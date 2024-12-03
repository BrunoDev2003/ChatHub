package com.chathub.chathub.repository;

import com.chathub.chathub.model.User;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class UsersRepository {
    private static final Logger LOGGER = Logger.getLogger(UsersRepository.class.getName());

    private static final String USERNAME_HASH_KEY = "username";
    private static final String USERNAME_KEY = "username:%s";
    private static final String ID_HASH_KEY = "id";
    private static final String IS_ONLINE_HASH_KEY = "isOnline";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, User> redisUserTemplate;

    public User getUserById(int id) {
        String usernameKey = String.format(USERNAME_KEY, id);
        String username = (String) redisTemplate.opsForHash().get(usernameKey, USERNAME_HASH_KEY);
        if (username == null) {
            LOGGER.warning("User not found");
            return null;
        }
        boolean isOnline = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(IS_ONLINE_HASH_KEY, String.valueOf(id)));
        return new User(id, username, isOnline);
    }

    public Set<Integer> getOnlineUsersIds() {
        Set<String> onlineUsersIds = redisTemplate.opsForSet().members(IS_ONLINE_HASH_KEY);
        if (onlineUsersIds == null) {
            LOGGER.warning("No online users found");
            return null;
        }
        return onlineUsersIds.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    public boolean userExists(String username) {
        return redisTemplate.hasKey(String.format(USERNAME_KEY, username));
    }

    public User getUserByName(String username) {
        if (!userExists(username)) {
            LOGGER.warning("User not found");
            return null;
        }
        String userKey = redisTemplate.opsForValue().get(String.format(USERNAME_KEY, username));
        int id = parseUserId(Objects.requireNonNull(userKey));
        boolean isOnline = redisTemplate.opsForSet().isMember(IS_ONLINE_HASH_KEY, String.valueOf(id));

        return new User(id, username, isOnline);

    }

    private int parseUserId(String userKey) {
        String[] userIds = userKey.split(":");
        return Integer.parseInt(userIds[1]);
    }

    public void addUserToOnlineList(String id) {
        redisTemplate.opsForSet().add(IS_ONLINE_HASH_KEY, id);
    }

    public void removeUserFromOnlineList(String id) {
        redisTemplate.opsForSet().remove(IS_ONLINE_HASH_KEY, id);
    }

    public List<User> findAll() {
        Set<String> keys = redisTemplate.keys("user:*");
        LOGGER.warning("Found keys: {}" + keys);

        List<User> users = new ArrayList<>();
        Gson gson = new Gson();
        for (String key : keys) {
            try {
                LOGGER.warning("Fetching value for key: {}" + key);
                String userJson = redisTemplate.opsForValue().get(key);
                User user = gson.fromJson(userJson, User.class);
                if (user != null) {
                    users.add(user);
                    LOGGER.warning("Added user: {}" + user);
                } else {
                    LOGGER.warning("No user found for key: " + key);
                }
            } catch (Exception e) {
                LOGGER.severe("Error fetching user for key: " + key);
            }
        }
        return users;
    }
}
