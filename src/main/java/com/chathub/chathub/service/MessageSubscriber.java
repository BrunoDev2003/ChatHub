package com.chathub.chathub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MessageSubscriber implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSubscriber.class);

    CopyOnWriteArrayList<Function<String, Integer>> handlers = new CopyOnWriteArrayList<>();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody());

        LOGGER.info("Mensagem recebida no global subscriber: " + message.toString());

        for (Function<String, Integer> handler : handlers) {
            handler.apply(messageBody);
        }
    }

    public void attach(Function<String, Integer> handler) {
        handlers.add(handler);
    }

    public void detach(Function<String, Integer> handler) {
        handlers.remove(handler);
    }
}
