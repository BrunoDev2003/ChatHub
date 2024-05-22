package com.chathub.chathub.controller;

import com.chathub.chathub.model.ChatRoomMessage;
import com.chathub.chathub.model.Room;
import com.chathub.chathub.repository.RoomsRepository;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.MessageSubscriber;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
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

        }
    }

    /*public ChatRoomMessage sendMessage(ChatRoomMessage message) {
        return new ChatRoomMessage(message.getFrom(), message.getDate(), message.getMessage(), message.getRoomId());
    }*/
}
