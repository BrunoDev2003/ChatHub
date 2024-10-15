package com.chathub.chathub.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class User implements Serializable {
    private int id;
    private String username;
    private boolean isOnline;

    public User(int id, String username, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.isOnline = isOnline;
    }
}
