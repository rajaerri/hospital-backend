package com.hospital.appointment.service;

import com.hospital.appointment.dto.AuthResponseDto;
import com.hospital.appointment.dto.LoginRequestDto;
import com.hospital.appointment.dto.RegisterRequestDto;
import com.hospital.appointment.dto.UserResponseDto;

/**
 * Authentication Service Interface
 */
public interface AuthService {

    UserResponseDto register(RegisterRequestDto requestDto);

    AuthResponseDto login(LoginRequestDto requestDto);
}
