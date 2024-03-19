package com.chathub.chathub.controller;

import com.chathub.chathub.model.ChatRoomMessage;
import com.chathub.chathub.repository.UsersRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
public class ChatRoomController {

    private static final LOGGER = LoggerFactory.getLogger(ChatRoomController .class);
    @Autowired
    private UsersRepository usersRepository;

    public ChatRoomMessage sendMessage(ChatRoomMessage message) {
        return new ChatRoomMessage(message.getFrom(), message.getDate(), message.getMessage(), message.getRoomId());
    }
}
