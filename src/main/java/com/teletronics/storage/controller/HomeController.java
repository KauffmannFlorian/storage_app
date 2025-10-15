package com.teletronics.storage.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Home", description = "Welcome endpoint for the Storage API")
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "Storage API is running. Visit /swagger-ui/index.html for documentation.";
    }
}

