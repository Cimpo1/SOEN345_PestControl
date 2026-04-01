package com.pestcontrol.backend.unit.api;

import com.pestcontrol.backend.api.AuthController;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.LoginResponse;
import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.api.dto.UserResponse;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.fullName = "testuser";
        request.password = "password";

        authController.registerUser(request);

        verify(authService, times(1)).register(request);
    }
    @Test
    void shouldLoginUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@email.com");
        request.setPassword("password");
        User user = new User();
        UserResponse userResponse = new UserResponse(user);

        LoginResponse mockResponse = new LoginResponse("mock-jwt-token", userResponse);

        when(authService.login(request)).thenReturn(mockResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("mock-jwt-token", response.getBody().getToken());

        verify(authService, times(1)).login(request);
    }
}
