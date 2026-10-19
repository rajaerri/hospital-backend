package com.hospital.appointment.controller;

import com.hospital.appointment.dto.*;
import com.hospital.appointment.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication REST Controller
 * 
 * Endpoints:
 * 1. POST /api/auth/register - Register a new patient account (BCrypt hashed, assigned ROLE_PATIENT)
 * 2. POST /api/auth/login    - Authenticate user credentials and return a signed JWT token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Patient Self-Registration API
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto requestDto) {
        UserResponseDto createdUser = authService.register(requestDto);
        ApiResponse<UserResponseDto> response = ApiResponse.success(
                "Patient registered successfully! You can now log in.",
                createdUser
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * User Login API (Issues JWT Token)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto requestDto) {
        AuthResponseDto authResponse = authService.login(requestDto);
        ApiResponse<AuthResponseDto> response = ApiResponse.success(
                "Login successful. JWT token issued.",
                authResponse
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
