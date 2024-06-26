package com.chathub.chathub.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class Room {
    private String id;
    private String[] names;

    public Room() {
        this.names = new String[2];
    }

    public Room(String id, String name) {
        this.id = id;
        this.names = new String[1];
        this.names[0] = name;
    }

    public Room(String id, String name1, String name2) {
        this.id = id;
        this.names = new String[2];
        this.names[0] = name1;
        this.names[1] = name2;
    }
}
