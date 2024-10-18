package com.chathub.chathub.controller;

import com.chathub.chathub.model.*;
import com.chathub.chathub.repository.RoomsRepository;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.MessageSubscriber;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
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
            LOGGER.debug("Mensagem recebida: " + message);
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(message);

            try {
                emitter.send(event);
            } catch (IOException e) {
                // Isso ocorre quando o cliente é desconectado
                LOGGER.error("Erro ao mandar mensagem", e);
                return 1;
            }
            return 0;
        };

        // RedisMessageSubscriber é uma classe global que realiza um subscribe no canal de "MESSAGES"
        // quando o /stream é invocado, é necessário notificar o subscriber global
        // que tal inscrição de cliente-servidor existe.
        // retornamos o callback para o subscriber com a instância SSE para mandar eventos server-side.

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

    @RequestMapping(value = "/emit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatRoomMessage>> getMessages() {
        //Coletar mensagens do repo ou DB
        List<ChatRoomMessage> messages = roomsRepository.getAllMessages();
        LOGGER.debug("Mensagens recebidas: " + messages.toString());

        if (messages.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.ok(messages);
        }
    }

    @RequestMapping(value = "/emit", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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

        LOGGER.debug("Mensagem serializada: " + serializedMessage);

        // Envia o serializedJson para o banco de dados Redis
        roomsRepository.sendMessageToDatabase(topic.getTopic(), serializedMessage);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private String handleRegularMessageCase(ChatRoomMessage message) {
        Gson gson = new Gson();
        LOGGER.info("Handling regular message case..." + message.getData());
        // após receber a mensagem do usuario, deserializa a mensagem
        LOGGER.debug("Deserializando mensagem..." + message.getData());
        Message chatmessage = gson.fromJson(message.getData(), Message.class);
        // adiciona o usuario que enviou a mensagem na lista de usuarios online
        usersRepository.addUserToOnlineList(chatmessage.getFrom());
        // salva a mensagem no BD
        roomsRepository.saveMessage(chatmessage);
        // serializa a mensagem para ser enviada por pub/sub
        String serializedMessage = gson.toJson(new PubSubMessage<>(message.getType().value(), message));
        LOGGER.debug("Mensagem serializada para Pub/Sub: " + serializedMessage);
        return serializedMessage;
    }

    private String handleUserConnectionCase(ChatRoomMessage message) {
        Gson gson = new Gson();
        int userId = message.getUser().getId();
        String messageType = message.getType().value();
        LOGGER.info("Handling user connection case..." + message.toString());

        try {
            User serializedUser = gson.fromJson(message.getData(), User.class);
            LOGGER.debug("Serializando usuario..." + serializedUser.toString());
            String serializedMessage = gson.toJson(new PubSubMessage<>(messageType, serializedUser));
            LOGGER.debug("Mensagem serializada para Pub/Sub: " + serializedMessage);
            if (message.getType() == MessageType.USER_CONNECTED) {
                usersRepository.addUserToOnlineList(String.valueOf(userId));
            } else {
                usersRepository.removeUserFromOnlineList(String.valueOf(userId));
            }
            return serializedMessage;

        } catch (JsonSyntaxException e) {
            LOGGER.error("Erro ao deserializar usuario" + message.getData(), e);
            throw e;
        }
    }

}
