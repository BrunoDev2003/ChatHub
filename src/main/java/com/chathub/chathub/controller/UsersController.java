package com.chathub.chathub.controller;

import com.chathub.chathub.config.SessionAttrs;
import com.chathub.chathub.model.User;
import com.chathub.chathub.repository.UsersRepository;
import com.chathub.chathub.service.UserService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @Autowired
    private UserService userService;

    /**
     * O request que o cliente faz para verificar se o usuário existe no cache.
     */
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, User>> get(@RequestParam(value = "ids") String idsString) {
        Set<Integer> ids = parseIds(idsString);

        Map<String, User> usersMap = new HashMap<>();

        for (Integer id : ids) {
            User user = usersRepository.getUserById(id);
            if (user == null) {
                LOGGER.debug("Usuário não encontrado pelo id: " + id);
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

    @RequestMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getMe(Model model, HttpSession session) {
        String user = (String) session.getAttribute(SessionAttrs.USER_ATTR_NAME);
        if (user == null) {
            LOGGER.debug("Usuario não encontrado na sessão pelo atributo desejado." + SessionAttrs.USER_ATTR_NAME);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Gson gson = new Gson();
        return new ResponseEntity<>(gson.fromJson(user, User.class), HttpStatus.OK);
    }

    /**
     * Valida qual usuarios estão online
     */

    @RequestMapping(value = "/online", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, User>> getOnlineUsers() {
        Map<String, User> usersMap = new HashMap<>();
        Set<Integer> onlineUsersIds = usersRepository.getOnlineUsersIds();
        if (onlineUsersIds == null) {
            LOGGER.debug("Nenhum usuário online encontrado.");
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        }
        for (Integer onlineId : onlineUsersIds) {
            User user = usersRepository.getUserById(onlineId);
            if (user != null) {
                LOGGER.debug("Usuário não encontrado pelo id: " + onlineId);
                return new ResponseEntity<>(new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            usersMap.put(String.valueOf(user.getId()), user);
        }

        return new ResponseEntity<>(usersMap, HttpStatus.OK);
    }

    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateUserStatus(@RequestParam("userId") String userId, @RequestParam("isOnline") boolean isOnline) {
        try{
            userService.updateUserStatus(userId, isOnline);
            return new ResponseEntity<>("Status do usuário atualizado com sucesso.", HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar o status do usuário.", e);
            return new ResponseEntity<>("Erro ao atualizar o status do usuário.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
