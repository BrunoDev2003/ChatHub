package com.chathub.chathub.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class RootController {
    @RequestMapping("/")
    public RedirectView main() {
        return new RedirectView("/index.html");
    }
}
