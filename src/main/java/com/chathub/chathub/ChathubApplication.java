package com.chathub.chathub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Collections;

@SpringBootApplication(scanBasePackages = "com.chathub.chathub")
public class ChathubApplication {

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        SpringApplication app = new SpringApplication(ChathubApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", port));
        app.run(args);

    }

}
