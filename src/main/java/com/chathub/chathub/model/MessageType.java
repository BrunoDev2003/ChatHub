package com.chathub.chathub.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    MESSAGE("message"),
    USER_CONNECTED("user_connected"),
    USER_DISCONNECTED("user_disconnected");

    private final String type;

    MessageType(String type) {
        this.type = type;
    }

    @JsonValue
    public String type() {
        return this.type;
    }
}
