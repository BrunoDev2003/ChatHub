package com.chathub.chathub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class MessageSubscriber implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSubscriber.class);
}
