package com.hospital.appointment.dto;

/**
 * Authentication Response DTO returned after successful login or registration
 */
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private Long expiresInMs;
    private UserResponseDto user;

    public AuthResponseDto() {
        this.tokenType = "Bearer";
    }

    public AuthResponseDto(String token, Long expiresInMs, UserResponseDto user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(Long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public UserResponseDto getUser() {
        return user;
    }

    public void setUser(UserResponseDto user) {
        this.user = user;
    }
}
