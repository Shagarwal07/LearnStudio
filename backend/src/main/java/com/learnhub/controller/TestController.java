package com.learnhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // Keep root (/) clear so Spring serves index.html from static resources
    @GetMapping("/api/test")
    public String test() {
        return "API Working";
    }
}