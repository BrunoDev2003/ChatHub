package com.chathub.chathub.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message {
    private String from;
    private long date;
    private String text;
    private String roomId;

    public Message(String from, long date, String text, String roomId) {
        this.from = from;
        this.date = date;
        this.text = text;
        this.roomId = roomId;
    }
}
