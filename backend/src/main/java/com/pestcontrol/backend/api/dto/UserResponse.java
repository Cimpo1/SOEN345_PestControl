package com.pestcontrol.backend.api.dto;

import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;

public class UserResponse {
    public Long userId;
    public String fullName;
    public String email;
    public String phoneNumber;
    public UserRole userRole;

    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.userRole = user.getUserRole();
    }
}