package com.hospital.appointment.service;

import com.hospital.appointment.dto.AuthResponseDto;
import com.hospital.appointment.dto.LoginRequestDto;
import com.hospital.appointment.dto.RegisterRequestDto;
import com.hospital.appointment.dto.UserResponseDto;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.UserRepository;
import com.hospital.appointment.security.JwtUtil;
import com.hospital.appointment.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Beginner-Friendly Unit Tests for AuthService
 * 
 * Demonstrates:
 * 1. Mockito isolation: Testing business logic without launching Spring Context or a database.
 * 2. Mocking dependencies with @Mock and injecting with @InjectMocks.
 * 3. Validating registration business rules (duplicate email check, BCrypt hashing).
 * 4. Validating login authentication and token issuance.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("UNIT: Successful Patient Registration -> Hashes Password & Persists ROLE_PATIENT")
    void testRegisterSuccess() {
        // Given (Arrange)
        RegisterRequestDto request = new RegisterRequestDto(
                "Alice Smith",
                "alice@example.com",
                "Password123",
                "5551234567",
                "A+",
                "456 Elm St"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedPasswordHere");

        User savedUser = new User(
                "Alice Smith",
                "alice@example.com",
                "$2a$10$hashedPasswordHere",
                "5551234567",
                Role.ROLE_PATIENT,
                "A+",
                "456 Elm St"
        );
        savedUser.setId(10L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When (Act)
        UserResponseDto response = authService.register(request);

        // Then (Assert)
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(Role.ROLE_PATIENT, response.getRole());

        // Verify that password was encoded and saved exactly once
        verify(passwordEncoder, times(1)).encode("Password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("UNIT: Register with Duplicate Email -> Throws IllegalArgumentException")
    void testRegisterDuplicateEmailThrowsException() {
        // Given
        RegisterRequestDto request = new RegisterRequestDto(
                "Alice Smith",
                "alice@example.com",
                "Password123",
                "5551234567",
                "A+",
                "456 Elm St"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // When & Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        // Ensure user was never saved
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("UNIT: Successful Login -> Authenticates & Issues JWT Token")
    void testLoginSuccess() {
        // Given
        LoginRequestDto request = new LoginRequestDto("alice@example.com", "Password123");

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        User existingUser = new User(
                "Alice Smith",
                "alice@example.com",
                "$2a$10$hash",
                "5551234567",
                Role.ROLE_PATIENT,
                "A+",
                "456 Elm St"
        );
        existingUser.setId(10L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(existingUser, "ROLE_PATIENT")).thenReturn("jwt.token.sample");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        // When
        AuthResponseDto response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt.token.sample", response.getToken());
        assertEquals(86400000L, response.getExpiresInMs());
        assertEquals("alice@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("UNIT: Login with Bad Credentials -> Throws BadCredentialsException")
    void testLoginBadCredentialsThrowsException() {
        // Given
        LoginRequestDto request = new LoginRequestDto("alice@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        // Verify token was never generated
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("UNIT: Login with Non-Existent User in Database -> Throws ResourceNotFoundException")
    void testLoginUserNotFoundThrowsException() {
        // Given
        LoginRequestDto request = new LoginRequestDto("ghost@example.com", "Password123");

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.login(request)
        );
    }
}
