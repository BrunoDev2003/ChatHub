package com.chathub.chathub;

import com.chathub.chathub.model.Message;
import com.chathub.chathub.model.Room;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.chathub.chathub.model.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class DemoDataCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataCreator.class);
    private StringRedisTemplate redisTemplate;
    private RedisTemplate<String, User> redisUserTemplate;

    @Autowired
    public DemoDataCreator(StringRedisTemplate redisTemplate, RedisTemplate<String, User> redisUserTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisUserTemplate = redisUserTemplate;
        this.createDemoData();
    }

    public static final String DEMO_PASSWORD = "senha123";
    private static final List<String> DEMO_USERNAME_LIST = Arrays.asList("Joe", "Bruno", "Mariana", "Alex");
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
                long messageDate = getTimestamp() - ((DEMO_MESSAGES_LIST.size() - messageIndex) * 200000); //subtrair por 200000 milisegundos (200 segundos);
                addMessage("0", getRandomUserId(users), DEMO_MESSAGES_LIST.get(messageIndex), messageDate);

            }
        }
    }

    private String getRandomGreeting() {
        return DEMO_GREETING_LIST.get((int) Math.floor(Math.random() * DEMO_GREETING_LIST.size()));
    }

    private long getTimestamp() {
        return System.currentTimeMillis(); // retorna o tempo atual em milisegundos;
    }

    private void addMessage(String roomId, String fromId, String content, long timeStamp) {
        Gson gson = new Gson();
        String roomKey = String.format("room:%s", roomId);
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
        String hashedPassword = encoder.encode(DEMO_PASSWORD);
        Integer nextId = redisTemplate.opsForValue().increment("total_users").intValue();
        String userKey = String.format("user:%s", nextId);
        String usernameKey = String.format("username:%s", username);

        // Chaves existentes são tratadas de forma correta;
        if (redisTemplate.hasKey(userKey)) {
            DataType type = redisTemplate.type(userKey);
            LOGGER.warn("Key {} exists with type: {}", userKey, type);
            if (!type.equals(DataType.STRING)) {
                LOGGER.error("Key type mismatch for {}. Expected STRING, found: {}", userKey, type);
                redisTemplate.delete(userKey); // Delete se a chave existir com um tipo diferente do que o sistema requer;
            }
        }

        // Salvar User Object corretamente em formato de uma String JSON usando a propriedade @class;
        Gson gson = new Gson();
        User user = new User(nextId, username, false);
//        String userJson = gson.toJson(new User(nextId, username, false));
//        String userJson = gson.toJson(user);
        redisUserTemplate.opsForValue().set(userKey, user);
        LOGGER.info("Stored user object as JSON in redisTemplate with key: {}", userKey);

        // Salvar nome de usuario e senha como chaves separadas para evitar conflitos de tipos de chaves;
        redisTemplate.opsForValue().set(usernameKey, userKey);
        redisTemplate.opsForHash().put(String.format("user:%s:details", nextId), "username", username);
        redisTemplate.opsForHash().put(String.format("user:%s:details", nextId), "password", hashedPassword);
        LOGGER.info("Stored username and password in redisTemplate for key: user:{}:details", nextId);

        // adicionar usuario a um set de rooms
        String roomsKey = String.format("user:%s:rooms", nextId);
        redisTemplate.opsForSet().add(roomsKey, "0");
        LOGGER.info("Added user to room set with key: {}", roomsKey);

        return user;
    }

    private String getPrivateRoomId(Integer userId1, Integer userId2) {
        Integer minUserId = userId1 > userId2 ? userId2 : userId1;
        Integer maxUserId = userId1 > userId2 ? userId1 : userId2;
        return String.format("%d:%d", minUserId, maxUserId);
    }

    private Room createPrivateRoom(Integer user1, Integer user2) {
        String roomId = getPrivateRoomId(user1, user2);

        String userRoomkey1 = String.format("user:%d:rooms", user1);
        String userRoomkey2 = String.format("user:%d:rooms", user2);

        // Deletar chaves antigas com tipo imcompativel para evitar conflitos
        if (redisTemplate.hasKey(userRoomkey1)) {
            DataType type = redisTemplate.type(userRoomkey1);
            if (!type.equals(DataType.SET)) {
                LOGGER.error("Key type mismatch for {}. Expected SET, found: {}", userRoomkey1, type);
                redisTemplate.delete(userRoomkey1);
            }
        }
        if (redisTemplate.hasKey(userRoomkey2)) {
            DataType type = redisTemplate.type(userRoomkey2);
            if (!type.equals(DataType.SET)) {
                LOGGER.error("Key type mismatch for {}. Expected SET, found: {}", userRoomkey2, type);
                redisTemplate.delete(userRoomkey2);
            }
        }

        redisTemplate.opsForSet().add(userRoomkey1, roomId);
        redisTemplate.opsForSet().add(userRoomkey2, roomId);

        String key1 = String.format("user:%d", user1);
        String key2 = String.format("user:%d", user2);

        return new Room(
                roomId,
                (String) redisTemplate.opsForHash().get(String.format("user:%d:details", user1), "username"),
                (String) redisTemplate.opsForHash().get(String.format("user%d:details", user2), "username")
        );
    }

    private long generateMessageDate() {
        return getTimestamp() - (long) (Math.random() * 222000); // Subtrair por 222000 milisegundos (222 segundos);
    }

    private String getRandomUserId(List<User> users) {
        return String.valueOf(users.get((int) Math.floor(users.size() * Math.random())).getId());
    }

}
