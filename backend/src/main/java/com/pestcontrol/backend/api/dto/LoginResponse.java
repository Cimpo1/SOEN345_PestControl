package com.pestcontrol.backend.api.dto;

public class LoginResponse {
    public String token;
    public UserResponse user;

    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }
}