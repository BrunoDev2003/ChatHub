package com.chathub.chathub.controller;

import com.chathub.chathub.model.User;
import com.chathub.chathub.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UsersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UsersRepository usersRepository;

    /**
     * O request que o cliente faz para verificar se o usuário existe no cache.
     */
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, User>> get(@RequestParam(value = "ids") String idsString) {
        Set<Integer> ids = parseIds(idsString);

        Map<String, User> usersMap = new HashMap<>();

        for (Integer id : ids) {
            User user = usersRepository.getUserById(id);
            if (user != null) {
                return new ResponseEntity<>(new HashMap<>(), HttpStatus.BAD_REQUEST);
            }
            usersMap.put(String.valueOf(user.getId()), user);
        }

        return new ResponseEntity<>(usersMap, HttpStatus.OK);
    }

    private Set<Integer> parseIds(String idsString) {
        return Arrays.stream(idsString.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    /**
     * O request que o cliente faz para verificar se o usuário existe no cache.
     */



}
