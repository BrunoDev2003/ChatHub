package com.chathub.chathub.controller;

import com.chathub.chathub.config.SessionAttrs;
import com.chathub.chathub.model.User;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.UserService;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.*;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UsersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, User> redisUserTemplate;
    private BlockingDeque<String> messageQueue = new LinkedBlockingDeque<>();

    private final Jedis jedis;

    @Autowired
    public UsersController(Jedis jedis) {
        this.jedis = jedis;
    }

    @PostConstruct
    public void initJedisSubscriber() {
        new Thread(() -> this.jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                LOGGER.info("Mensagem recebida no global subscriber: " + message + "No canal: " + channel);
                if ("user-status".equals(channel)) {
                    messageQueue.offer(message);
                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                LOGGER.info("Subscrito ao canal: " + channel);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                LOGGER.info("Desinscrito do canal: " + channel);
            }
        }, "user-status")).start();
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAllUsers() {
        Set<String> keys = redisUserTemplate.keys("user:*");
        List<User> users = new ArrayList<>();
        for (String key : keys) {
            if (key.endsWith(":details") || key.endsWith(":rooms")) {
                continue; //pular chaves que não são de usuários;
            }
            try {
                User user = redisUserTemplate.opsForValue().get(key);
                if (user != null) {
                    users.add(user);
                    LOGGER.info("Usuário encontrado e adicionado ao array de users : " + user);
                } else {
                    LOGGER.warn("Usuário não encontrado pela chave: " + key);
                }
            } catch (Exception e) {
                LOGGER.error("Erro ao buscar usuário pela chave: {} {}", e, key);
            }
        }
        return ResponseEntity.ok(users);
    }

    /**
     * O request que o cliente faz para verificar se o usuário existe no cache pela sua Id.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, User>> get(@RequestParam(value = "ids") String idsString) {
        Set<Integer> ids = parseIds(idsString);

        Map<String, User> usersMap = new HashMap<>();

        for (Integer id : ids) {
            User user = usersRepository.getUserById(id);
            if (user == null) {
                LOGGER.debug("Usuário não encontrado pelo id: " + id);
                return new ResponseEntity<>(new HashMap<>(), HttpStatus.BAD_REQUEST);
            }
            usersMap.put(String.valueOf(user.getId()), user);
        }

        return new ResponseEntity<>(usersMap, HttpStatus.OK);
    }

    private Set<Integer> parseIds(String idsString) {
        return Arrays.stream(idsString.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    /**
     * O request que o cliente faz para verificar se o usuário existe em uma sessão do Redis.
     */

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getMe(Model model, HttpSession session) {
        String user = (String) session.getAttribute(SessionAttrs.USER_ATTR_NAME);
        if (user == null) {
            LOGGER.debug("Usuario não encontrado na sessão pelo atributo desejado." + SessionAttrs.USER_ATTR_NAME);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Gson gson = new Gson();
        return new ResponseEntity<>(gson.fromJson(user, User.class), HttpStatus.OK);
    }

    /**
     * Valida qual usuarios estão online
     */

    @GetMapping(value = "/online", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, User>> getOnlineUsers() {
        Map<String, User> usersMap = new HashMap<>();
        Set<Integer> onlineUsersIds = usersRepository.getOnlineUsersIds();
        if (onlineUsersIds == null) {
            LOGGER.debug("Nenhum usuário online encontrado.");
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        }
        for (Integer onlineId : onlineUsersIds) {
            User user = usersRepository.getUserById(onlineId);
            if (user != null) {
                LOGGER.debug("Usuário não encontrado pelo id: " + onlineId);
                return new ResponseEntity<>(new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            usersMap.put(String.valueOf(user.getId()), user);
        }

        return new ResponseEntity<>(usersMap, HttpStatus.OK);
    }

    @PostMapping(value = "/updateStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateUserStatus(@RequestParam("userId") String userId, @RequestParam("isOnline") boolean isOnline) {
        try {
            userService.updateUserStatus(userId, isOnline);
            return new ResponseEntity<>("Status do usuário atualizado com sucesso.", HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar o status do usuário.", e);
            return new ResponseEntity<>("Erro ao atualizar o status do usuário.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //Endpoint para obter atualizações de status como uma nova mensagem do usuário, atualização que busca a fila de mensagens a cada 5 segundos no máximo.

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/status-updates")
    public ResponseEntity<String> getStatusUpdates() {
        try {
            String message = messageQueue.poll(5, TimeUnit.SECONDS);
            if (message == null) {
                return new ResponseEntity<>("Nenhuma atualização de status disponível.", HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseEntity<>("Erro ao obter atualizações de status.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
