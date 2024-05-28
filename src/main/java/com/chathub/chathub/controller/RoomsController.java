package com.chathub.chathub.controller;

import com.chathub.chathub.model.Room;
import com.chathub.chathub.repository.RoomsRepository;
import com.chathub.chathub.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/rooms")
public class RoomsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomsController.class);

    @Autowired
    private RoomsRepository roomsRepository;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Pegar rooms para o id specifico do usuario.
     */

    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Room>> getRooms(@PathVariable int userId) {
        Set<String> roomIds = roomsRepository.getUserRoomIds(userId);
        if (roomIds == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        List<Room> rooms = new ArrayList<>();

        for (String roomId : roomIds) {
            boolean roomExists = roomsRepository.roomExists(roomId);
            if (roomExists){
                String name = roomsRepository.getRoomNameById(roomId);
                if (name == null) {
                    // private chat case
                    Room privateRoom = handlePrivateRoomCase(roomId);
                    if (privateRoom == null){
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    rooms.add(privateRoom);
                } else {
                    rooms.add(new Room(roomId, name));
                }
            }
        }
        return new ResponseEntity<>(rooms, HttpStatus.OK);

    }

    private String[] parseUserIds(String roomId){
        String[] userIds = roomId.split(":");
        if (userIds.length != 2){
            LOGGER.error("User ids not parsed properly");
            throw new RuntimeException("Unable to parse users ids from roomId: "+roomId);
        }
        return userIds;
    }


}
