package com.chathub.chathub.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    MESSAGE("message"),
    USER_CONNECTED("user_connected"),
    USER_DISCONNECTED("user_disconnected");

    private final String value;

    MessageType(String type) {
        this.value = type;
    }

    @JsonValue
    public String value() {
        return this.value;
    }
}
