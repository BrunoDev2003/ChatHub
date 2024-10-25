package com.chathub.chathub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static MessageType forValue(String value) {
        for (MessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }
}
