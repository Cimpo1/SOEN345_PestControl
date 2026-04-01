package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.LoginResponse;
import com.pestcontrol.backend.api.dto.UserResponse;
import com.pestcontrol.backend.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void testConstructorAndGetters() {
        String token = "jwt-token-123";
        User user = new User();
        UserResponse userResponse = new UserResponse(user); // assuming default constructor exists

        LoginResponse response = new LoginResponse(token, userResponse);

        assertEquals(token, response.getToken());
        assertEquals(userResponse, response.getUser());
    }

    @Test
    void testTokenSetterAndGetter() {
        LoginResponse response = new LoginResponse(null, null);

        String token = "new-token";
        response.setToken(token);

        assertEquals(token, response.getToken());
    }

    @Test
    void testUserSetterAndGetter() {
        LoginResponse response = new LoginResponse(null, null);

        User user = new User();
        UserResponse userResponse = new UserResponse(user);
        response.setUser(userResponse);

        assertEquals(userResponse, response.getUser());
    }

    @Test
    void testAllFieldsTogether() {
        LoginResponse response = new LoginResponse(null, null);

        String token = "full-test-token";
        User user = new User();
        UserResponse userResponse = new UserResponse(user);

        response.setToken(token);
        response.setUser(userResponse);

        assertAll(
                () -> assertEquals(token, response.getToken()),
                () -> assertEquals(userResponse, response.getUser())
        );
    }
}
