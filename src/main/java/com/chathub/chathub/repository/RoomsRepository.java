package com.chathub.chathub.repository;

import com.chathub.chathub.model.ChatRoomMessage;
import com.chathub.chathub.model.Message;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
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

    public Set<String> getMessages(String roomId, int offset, int size) {
        String roomKey = String.format(ROOMS_KEY, roomId);
        Set<String> messages = redisTemplate.opsForZSet().reverseRange(roomKey, offset, offset + size);
        LOGGER.debug(String.format("Mensagens recebidas da roomId:%s:, offset:%s, size:%s ", roomId, offset, size));
        return redisTemplate.opsForZSet().reverseRange(roomKey, offset, offset + size);
    }

    public List<Message> getMessagesByRoomId(String roomId) {
        String roomKey = String.format(ROOMS_KEY, roomId);
        LOGGER.info("Getting messages for roomKey: {}", roomKey);
        Set<String> messages = redisTemplate.opsForZSet().reverseRange(roomKey, 0, -1);
        if (messages == null || messages.isEmpty()) {
            LOGGER.warn("No messages found for roomId: {}", roomId);
            return Collections.emptyList();
        }
        return messages.stream().map(json -> new Gson().fromJson(json, Message.class)).collect(Collectors.toList());
    }


    public List<ChatRoomMessage> getAllMessages() {
        // Assuming you store messages with a known key pattern
        Set<String> messages = redisTemplate.opsForZSet().reverseRange("room:messages", 0, -1);
        return messages.stream().map(json -> new Gson().fromJson(json, ChatRoomMessage.class)).collect(Collectors.toList());
    }


    public void sendMessageToDatabase(String text, String serializedMessage) {
        LOGGER.debug(String.format("Salvando mensagem no banco de dados: topico:%s, mensagem:%s ", text, serializedMessage));
        redisTemplate.convertAndSend(text, serializedMessage);
    }

    public void saveMessage(Message message) {
        Gson gson = new Gson();
        String roomKey = String.format(ROOMS_KEY, message.getRoomId());
        redisTemplate.opsForZSet().add(roomKey, gson.toJson(message), message.getDate());
    }

    public List getMessagesByUserId(int userId) {
        Set<String> roomIds = getUserRoomIds(userId);
        List<ChatRoomMessage> userMessages = new ArrayList<>();
        for (String roomId : roomIds) {
            Set<String> messages = getMessages(roomId, 0, -1);
            messages.forEach(json -> userMessages.add(new Gson().fromJson(json, ChatRoomMessage.class)));
        }
        return userMessages;
    }


}
