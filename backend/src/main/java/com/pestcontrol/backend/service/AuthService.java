package com.pestcontrol.backend.service;

import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.UserResponse;
import com.pestcontrol.backend.api.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private UserRepository userRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhoneNumber = normalizeValue(request.getPhoneNumber());

        if (normalizedEmail == null && normalizedPhoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password required");
        }

        // Duplicates
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (normalizedPhoneNumber != null && userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setPasswordHash(passwordHash);
        user.setUserRole(UserRole.CUSTOMER);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhoneNumber = normalizeValue(request.getPhoneNumber());

        if (normalizedEmail == null && normalizedPhoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credentials");
        }

        User user;
        if (normalizedEmail != null) {
            user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        } else {
            user = userRepository.findByPhoneNumber(normalizedPhoneNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = JWTService.generateToken(user);
        return new LoginResponse(token, new UserResponse(user));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.toLowerCase();
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
