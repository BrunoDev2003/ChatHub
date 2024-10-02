package com.chathub.chathub.controller;

import com.chathub.chathub.config.SessionAttrs;
import com.chathub.chathub.model.LoginData;
import com.chathub.chathub.model.User;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.UserService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")

public class AuthController {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserService userService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /**
     * Criar sessão do usuario pelo username e senha.
     */
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginData loginData, HttpSession session) {
        LOGGER.info("Login attempt: POST");

        if (loginData == null) {
            LOGGER.info("LoginData is NULL");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        LOGGER.info("Received LoginData: username={}, password={}", loginData.getUsername(), loginData.getPassword());

        String username = loginData.getUsername();

        boolean userExists = usersRepository.userExists(username);
        LOGGER.info("User exists: {}", userExists);
        if (!userExists) {
            LOGGER.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = usersRepository.getUserByName(username);
        if (user == null) {
            LOGGER.error("Unauthorized access for user: {}", username);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        user.setOnline(true);

        userService.updateUserStatus(String.valueOf(user.getId()), true);

        Gson gson = new Gson();
        session.setAttribute(SessionAttrs.USER_ATTR_NAME, gson.toJson(user));
        LOGGER.info("Logar usuario: " + user.getUsername());

        return new ResponseEntity<>(user, HttpStatus.OK);

    }

    @GetMapping("/login")
    public ResponseEntity<String> handleGetLogin() {
        LOGGER.info("Login attempt: GET");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("GET method not allowed for /login");
    }

    /**
     * Desligar a sessão do usuário.
     */
    @PostMapping(value = "/logout")
    public ResponseEntity<Object> logout(Model model, HttpSession session) {
        Object userObj = session.getAttribute(SessionAttrs.USER_ATTR_NAME);
        if (userObj == null) {
            LOGGER.info("Usuario não encontrado na sessão.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = new Gson().fromJson(userObj.toString(), User.class);
        LOGGER.info("Deslogar usuario: " + user.toString());

        userService.updateUserStatus(String.valueOf(user.getId()), false);

        session.removeAttribute(SessionAttrs.USER_ATTR_NAME);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Backend is running");
    }
}


