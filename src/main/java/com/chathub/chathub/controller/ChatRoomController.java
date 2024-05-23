package com.chathub.chathub.controller;

import com.chathub.chathub.model.*;
import com.chathub.chathub.repository.RoomsRepository;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.MessageSubscriber;
import com.google.gson.Gson;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRoomController.class);
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private Room room;

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    ChannelTopic topic;

    @Autowired
    MessageListenerAdapter messageListener;

    @RequestMapping("/stream")
    public SseEmitter streamSseMvc(@RequestParam int userId) {
        AtomicBoolean isComplete = new AtomicBoolean(false);
        SseEmitter emitter = new SseEmitter();

        Function<String, Integer> handler = (String message) -> {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(message);

            try {
                emitter.send(event);
            } catch (IOException e) {
                // Isso ocorre quando o cliente é desconectado
                LOGGER.error("Error sending message", e);
                return 1;
            }
            return 0;
        };

        // RedisMessageSubscriber is a global class which subscribes to the "MESSAGES" channel
        // However once the /stream endpoint is invoked, it's necessary to notify the global subscriber
        // that such client-server subscription exists.
        //
        // We send the callback to the subscriber with the SSE instance for sending server-side events.

        MessageSubscriber messageSubscriber = (MessageSubscriber) messageListener.getDelegate();
        messageSubscriber.attach(handler);

        Runnable onDetach = () -> {

            messageSubscriber.detach(handler);
            if (!isComplete.get()) {
                isComplete.set(true);
                emitter.complete();

            }
        };

        emitter.onCompletion(onDetach);
        emitter.onError((err) -> onDetach.run());
        emitter.onTimeout(onDetach);

        return emitter;

    /*public ChatRoomMessage sendMessage(ChatRoomMessage message) {
        return new ChatRoomMessage(message.getFrom(), message.getDate(), message.getMessage(), message.getRoomId());
    }*/
    }

    /**
     * Recebe mensagens do cliente...
     */

    @RequestMapping(value = "/emit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> get(@RequestBody ChatRoomMessage message) {
        Gson gson = new Gson();
        String serializedMessage;

        LOGGER.info("Mensagem recebida: " + message.toString());

        if (message.getType() == MessageType.MESSAGE) {
            serializedMessage = handleRegularMessageCase(message);
        } else if (message.getType() == MessageType.USER_CONNECTED
                || message.getType() == MessageType.USER_DISCONNECTED) {
            serializedMessage = handleUserConnectionCase(message);
        } else {
            serializedMessage = gson.toJson(new PubSubMessage<>(message.getType().value(), message.getData()));
        }

        // Envia o serializedJson para o banco de dados Redis
        roomsRepository.sendMessageToDatabase(topic.getTopic(), serializedMessage);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private String handleRegularMessageCase(ChatRoomMessage message) {
        Gson gson = new Gson();
        // We've received a message from user. It's necessary to deserialize it first.
        Message chatmessage = gson.fromJson(message.getData(), Message.class);
        // Add the user who sent the message to online list.
        usersRepository.addUserToOnlineList(chatmessage.getFrom());
        //redisTemplate.opsForSet().add(ONLINE_USERS_KEY, message.getFrom());
        // Write the message to DB.
        roomsRepository.saveMessage(chatmessage);
        // Finally create the serialized output which would go to pub/sub
        return gson.toJson(new PubSubMessage<>(message.getType().value(), message));
    }

    private String handleUserConnectionCase(ChatRoomMessage message) {
        Gson gson = new Gson();
        int userId = message.getUser().getId();
        String messageType = message.getType().value();
        User serializedUser = gson.fromJson(message.getData(), User.class);
        String serializedMessage = gson.toJson(new PubSubMessage<>(messageType, serializedUser));
        if (message.getType() == MessageType.USER_CONNECTED) {
            usersRepository.addUserToOnlineList(String.valueOf(userId));
        } else {
            usersRepository.removeUserFromOnlineList(String.valueOf(userId));
        }
        return serializedMessage;
    }

}
