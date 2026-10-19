package com.hospital.appointment.service.impl;

import com.hospital.appointment.dto.AuthResponseDto;
import com.hospital.appointment.dto.LoginRequestDto;
import com.hospital.appointment.dto.RegisterRequestDto;
import com.hospital.appointment.dto.UserResponseDto;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.UserRepository;
import com.hospital.appointment.security.JwtUtil;
import com.hospital.appointment.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service Implementation
 * 
 * Enforces:
 * 1. BCrypt password hashing before database persistence
 * 2. Automatic and strict ROLE_PATIENT assignment (prevents self-admin creation)
 * 3. Authentication via Spring Security's AuthenticationManager
 * 4. Cryptographic JWT issuance
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserResponseDto register(RegisterRequestDto requestDto) {
        // Check for duplicate email
        if (userRepository.existsByEmail(requestDto.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("An account with email '" + requestDto.getEmail() + "' already exists.");
        }

        // Hash plaintext password with BCrypt
        String hashedPassword = passwordEncoder.encode(requestDto.getPassword());

        // Construct User entity - strictly assign ROLE_PATIENT
        User newUser = new User(
                requestDto.getFullName().trim(),
                requestDto.getEmail().trim().toLowerCase(),
                hashedPassword,
                requestDto.getPhone().trim(),
                Role.ROLE_PATIENT, // Guarantees self-registration can never assign ROLE_ADMIN
                requestDto.getBloodGroup(),
                requestDto.getAddress()
        );

        User savedUser = userRepository.save(newUser);
        return mapToDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto requestDto) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();

        try {
            // Authenticate against Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            requestDto.getPassword()
                    )
            );

            // Fetch authenticated user details
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedEmail));

            // Generate cryptographically signed JWT token
            String jwtToken = jwtUtil.generateToken(user, user.getRole().name());

            return new AuthResponseDto(
                    jwtToken,
                    jwtUtil.getExpirationMs(),
                    mapToDto(user)
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password. Please verify your credentials and try again.");
        }
    }

    private UserResponseDto mapToDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getBloodGroup(),
                user.getAddress()
        );
    }
}
