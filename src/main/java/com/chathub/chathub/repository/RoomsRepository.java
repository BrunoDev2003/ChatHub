package com.chathub.chathub.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

public class RoomsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomsRepository.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String ROOMS_USER_KEY = "rooms:%d:users";
    private static final String ROOMS_NAME_KEY = "rooms:%s:name";
    private static final String ROOMS_KEY = "rooms:%s";

    public Set<String> getUserRoomIds(int userId) {
        String userRoomsKey = String.format(ROOMS_USER_KEY, userId);
        Set<String> roomIds = redisTemplate.opsForSet().members(userRoomsKey);
        LOGGER.debug("RoomIds recebidas pela userId: " + roomIds);
        return roomIds;
    }

    public boolean roomExists(String roomId) {
        return redisTemplate.hasKey(String.format(ROOMS_KEY, roomId));
    }

    public String getRoomNameById(String roomId) {
        String roomName = String.format(ROOMS_NAME_KEY, roomId);
        return redisTemplate.opsForValue().get(roomName);
    }


}
