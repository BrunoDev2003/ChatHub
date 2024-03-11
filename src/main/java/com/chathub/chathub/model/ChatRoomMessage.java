package com.chathub.chathub.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString

public class ChatRoomMessage {
    private String from;
    private int date;
    private String message;
    private String roomId;


}
