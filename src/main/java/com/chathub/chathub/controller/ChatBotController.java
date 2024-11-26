package com.chathub.chathub.controller;

import com.chathub.chathub.model.Message;
import com.chathub.chathub.repository.RoomsRepository;
import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@RestController
@RequestMapping("/chat/bot")
public class ChatBotController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatBotController.class);
    private String openaiApiKey;
    @Autowired
    private RoomsRepository roomsRepository;

    public ChatBotController() {
        // Load dotenv configuration in the constructor
        Dotenv dotenv = Dotenv.configure().directory("C:\\Users\\wwwbr\\chathub\\chathub").load();
        this.openaiApiKey = dotenv.get("OPENAI_API_KEY");
        LOGGER.info("OpenAI API Key loaded successfully.");
    }

    @PostMapping
    public ResponseEntity<String> getBotResponse(@RequestBody Message message) {
        LOGGER.info("OpenAI API Key: " + openaiApiKey);
        String userMessage = message.getText(); // Assuming the message text is in the "text" field
        String botResponse = getAIResponse(userMessage);
        Message botMessage = new Message("bot", System.currentTimeMillis(), botResponse, message.getRoomId());
        roomsRepository.saveMessage(botMessage);
        roomsRepository.sendMessageToDatabase(message.getRoomId(), new Gson().toJson(botMessage));
        return ResponseEntity.ok(botResponse);
    }

    private String getAIResponse(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://api.openai.com/v1/engines/gpt-3.5-turbo/completions";
        JSONObject request = new JSONObject();
        request.put("prompt", userMessage);
        request.put("max_tokens", 150);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        JSONObject responseBody = new JSONObject(response.getBody());
        String botResponse = responseBody.getJSONArray("choices").getJSONObject(0).getString("text").trim();
        return botResponse;
    }
}

