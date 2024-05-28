package com.chathub.chathub.controller;

import com.chathub.chathub.model.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

public class LinksController {
    @GetMapping(value = "/links", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Link> getMe() {
        return new ResponseEntity<Link>(new Link(), HttpStatus.OK);
    }
}
