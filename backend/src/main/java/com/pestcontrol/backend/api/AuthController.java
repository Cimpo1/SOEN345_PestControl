package com.pestcontrol.backend.api;

import com.pestcontrol.backend.api.DTOs.RegisterRequest;
import com.pestcontrol.backend.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public void registerUser(@RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
    }


}
