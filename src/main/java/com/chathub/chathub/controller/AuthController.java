package com.chathub.chathub.controller;

import com.chathub.chathub.config.SessionAttrs;
import com.chathub.chathub.model.LoginData;
import com.chathub.chathub.model.User;
import com.chathub.chathub.repository.UsersRepository;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class AuthController {
    @Autowired
    private UsersRepository usersRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /**
     * Criar sessão do usuario pelo username e senha.
     */
    @PostMapping(value = "/login")
    public ResponseEntity<User> login(@RequestBody LoginData loginData, HttpSession session) {
        String username = loginData.getUsername();

        boolean userExists = usersRepository.userExists(username);
        if (!userExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = usersRepository.getUserByName(username);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        user.setOnline(true);

        Gson gson = new Gson();
        session.setAttribute(SessionAttrs.USER_ATTR_NAME, gson.toJson(user));
        LOGGER.info("Logar usuario: " + user.getUsername());

        return new ResponseEntity<>(user, HttpStatus.OK);

    }

    /**
     * Desligar a sessão do usuário.
     */
    @PostMapping(value = "/logout")
    public ResponseEntity<Object> logout(Model model, HttpSession session) {
        Object user = session.getAttribute(SessionAttrs.USER_ATTR_NAME);
        LOGGER.info("Deslogar usuario: " + user.toString());

        session.removeAttribute(SessionAttrs.USER_ATTR_NAME);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Backend is running");
    }
}


