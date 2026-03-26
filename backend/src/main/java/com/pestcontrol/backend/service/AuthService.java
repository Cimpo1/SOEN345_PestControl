package com.pestcontrol.backend.service;

import com.pestcontrol.backend.api.DTOs.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private UserRepository userRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        if (request.email == null && request.phoneNumber == null) {
            throw new RuntimeException("Email or phone required");
        }

        //Duplicates
        if (request.email != null && userRepository.existsByEmail(request.email)) {
            throw new RuntimeException("Email already exists");
        }
        if (request.phoneNumber != null && userRepository.existsByPhoneNumber(request.phoneNumber)) {
            throw new RuntimeException("Phone already exists");
        }

        String passwordHash = passwordEncoder.encode(request.password);

        User user = new User();
        user.setFullName(request.fullName);
        user.setEmail(request.email);
        user.setPhoneNumber(request.phoneNumber);
        user.setPasswordHash(passwordHash);
        user.setUserRole(UserRole.CUSTOMER);
        userRepository.save(user);
    }
}
