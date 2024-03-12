package com.chathub.chathub.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomMessage {
    private User user;
    private MessageType type;
    private String data;

}
