package com.chathub.chathub.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@JsonTypeName("com.chathub.chathub.model.ChatRoomMessage")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChatRoomMessage.class, name = "ChatRoomMessage")
})
public class ChatRoomMessage {
    private User user;
    private MessageType type;
    private String data;

}
