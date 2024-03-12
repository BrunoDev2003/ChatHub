package com.chathub.chathub.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomMessage {
    private String user;
    private String type;
    private String data;
    private String roomId;


}
