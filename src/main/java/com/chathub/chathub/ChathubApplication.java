package com.chathub.chathub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ChathubApplication {

    public static void main(String[] args) {
	/*String port = System.getenv("PORT");
	if (port == null) {
		port = "8080";
	}*/
        //SpringApplication app = new SpringApplication(ChathubApplication.class);
        //app.setDefaultProperties(Collections.singletonMap("server.port", port));
        //app.run(args);
        SpringApplication.run(ChathubApplication.class, args);
    }
}
