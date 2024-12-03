package com.chathub.chathub.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

@JsonTypeName("com.chathub.chathub.model.User")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
@Getter
@Setter
public class User {
    private int id;
    private String username;
    private boolean isOnline;

    public User() {
        // Default no-argument constructor
    }

    public User(int id, String username, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.isOnline = isOnline;
    }
}
