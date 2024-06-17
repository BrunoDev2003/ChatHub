package com.chathub.chathub;

import com.chathub.chathub.model.Message;
import com.chathub.chathub.model.Room;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.chathub.chathub.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class DemoDataCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataCreator.class);

    private StringRedisTemplate redisTemplate;

    @Autowired
    public DemoDataCreator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.createDemoData();
    }

    private static final String DEMO_PASSWORD = "senha123";
    private static final List<String> DEMO_USERNAME_LIST = Arrays.asList("Pablo", "Joe", "Mary", "Alex");
    private static final List<String> DEMO_GREETING_LIST = Arrays.asList("Hello", "Hi", "Yo", "Hola");
    private static final List<String> DEMO_MESSAGES_LIST = Arrays.asList("Hello", "Hi", "Yo", "Hola");

    private void createDemoData() {
        //armazenamos um contador para o total de usuarios e incrementamos ele a cada registro

        final AtomicBoolean totalUsersKeyExist = new AtomicBoolean(redisTemplate.hasKey("total_users")); // Client.KeyExistsAsync("total_users");
        if (!totalUsersKeyExist.get()) {
            LOGGER.info("Inicializando demo data...\n");
            // este contador é usado para o roomId
            redisTemplate.opsForValue().set("total_users", "0");
            // Rooms com mensagens privadas não tem nome
            redisTemplate.opsForValue().set("room:0:name", "General");

            List<User> users = new LinkedList<>();
            // For each name create a user.
            // Para cada nome cria um usuario com o for each
            for (String username : DEMO_USERNAME_LIST) {
                User user = createUser(username);
                // usuario entra na seção/session
                users.add(user);
            }

            Map<String, Room> rooms = new HashMap<>();
            for (User user : users) {
                List<User> otherUsers = users.stream().filter(x -> x.getId() != user.getId()).collect(Collectors.toList());
                for (var otherUser : otherUsers) {
                    String privateRoomId = getPrivateRoomId(user.getId(), otherUser.getId());
                    Room room;
                    if (!rooms.containsKey(privateRoomId)) {
                        room = createPrivateRoom(user.getId(), otherUser.getId());
                        rooms.put(privateRoomId, room);
                    }
                    addMessage(privateRoomId, String.valueOf(otherUser.getId()), getRandomGreeting(), generateMessageDate());
                }
            }

            for (int messageIndex = 0; messageIndex < DEMO_MESSAGES_LIST.size(); messageIndex++) {
                int messageDate = getTimestamp() - ((DEMO_MESSAGES_LIST.size() - messageIndex) * 200);
                addMessage("0", getRandomUserId(users), DEMO_MESSAGES_LIST.get(messageIndex), messageDate);

            }
        }
    }

    private String getRandomGreeting() {
        return DEMO_GREETING_LIST.get((int) Math.floor(Math.random() * DEMO_GREETING_LIST.size()));
    }

    private int getTimestamp(){
        return Long.valueOf((System.currentTimeMillis() / 1000L)).intValue();
    }

    private void addMessage(String roomId, String fromId, String content, Integer timeStamp) {
        Gson gson = new Gson();
        String roomKey = String.format("room:%s", roomId);

        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timeStamp, 0, java.time.ZoneOffset.UTC);
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Message message = new Message(
                fromId,
                timeStamp,
                content,
                roomId
        );
        redisTemplate.opsForZSet().add(roomKey, gson.toJson(message), message.getDate());
    }

    private User createUser(String username) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String usernameKey = String.format("username:%s", username);

        // Usando o bcrypt para a senha.
        String hashedPassword = encoder.encode(DEMO_PASSWORD);

        Integer nextId = redisTemplate.opsForValue().increment("total_users").intValue();
        String userKey = String.format("user:%s", nextId);

        redisTemplate.opsForValue().set(usernameKey, userKey);
        redisTemplate.opsForHash().put(userKey, "username", username);
        redisTemplate.opsForHash().put(userKey, "password", hashedPassword);

        String roomsKey = String.format("user:%s:rooms", nextId);
        redisTemplate.opsForSet().add(roomsKey, "0");

        return new User(
                nextId,
                username,
                false
        );
    }

    private String getPrivateRoomId(Integer userId1, Integer userId2) {
        Integer minUserId = userId1 > userId2 ? userId2 : userId1;
        Integer maxUserId = userId1 > userId2 ? userId1 : userId2;
        return String.format("%d:%d", minUserId, maxUserId);
    }

    ;

    private Room createPrivateRoom(Integer user1, Integer user2) {
        String roomId = getPrivateRoomId(user1, user2);

        String userRoomkey1 = String.format("user:%d:rooms", user1);
        String userRoomkey2 = String.format("user:%d:rooms", user2);

        redisTemplate.opsForSet().add(userRoomkey1, roomId);
        redisTemplate.opsForSet().add(userRoomkey2, roomId);

        String key1 = String.format("user:%d", user1);
        String key2 = String.format("user:%d", user2);

        return new Room(
                roomId,
                (String) redisTemplate.opsForHash().get(key1, "username"),
                (String) redisTemplate.opsForHash().get(key2, "username")
        );
    }

    private int generateMessageDate() {
        return (int) (getTimestamp() - Math.random() * 222);
    }

    private String getRandomUserId(List<User> users) {
        return String.valueOf(users.get((int) Math.floor(users.size() * Math.random())).getId());
    }

}
