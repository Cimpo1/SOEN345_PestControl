package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void testEmailGetterAndSetter() {
        LoginRequest loginRequest = new LoginRequest();

        String email = "test@example.com";
        loginRequest.setEmail(email);

        assertEquals(email, loginRequest.getEmail());
    }

    @Test
    void testPhoneNumberGetterAndSetter() {
        LoginRequest loginRequest = new LoginRequest();

        String phoneNumber = "+1234567890";
        loginRequest.setPhoneNumber(phoneNumber);

        assertEquals(phoneNumber, loginRequest.getPhoneNumber());
    }

    @Test
    void testPasswordGetterAndSetter() {
        LoginRequest loginRequest = new LoginRequest();

        String password = "securePassword123";
        loginRequest.setPassword(password);

        assertEquals(password, loginRequest.getPassword());
    }

    @Test
    void testAllFieldsTogether() {
        LoginRequest loginRequest = new LoginRequest();

        String email = "user@test.com";
        String phone = "+1987654321";
        String password = "pass123";

        loginRequest.setEmail(email);
        loginRequest.setPhoneNumber(phone);
        loginRequest.setPassword(password);

        assertAll(
                () -> assertEquals(email, loginRequest.getEmail()),
                () -> assertEquals(phone, loginRequest.getPhoneNumber()),
                () -> assertEquals(password, loginRequest.getPassword())
        );
    }

}
