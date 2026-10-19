package com.hospital.appointment.service;

import com.hospital.appointment.dto.PatientResponseDto;
import com.hospital.appointment.dto.PatientUpdateRequestDto;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.UserRepository;
import com.hospital.appointment.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Beginner-Friendly Unit Tests for PatientService
 * 
 * Verifies profile viewing, update, and deletion rules using Mockito:
 * 1. Profile retrieval of authenticated patient
 * 2. Updating allowed demographic fields
 * 3. Enforcing cross-patient security (patients cannot view or modify other patients)
 * 4. Account self-deletion
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientServiceImpl patientService;

    private User patientUser;

    @BeforeEach
    void setUp() {
        patientUser = new User(
                "Alex Morgan",
                "alex@example.com",
                "$2a$10$hashedPass",
                "5552345678",
                Role.ROLE_PATIENT,
                "O+",
                "742 Evergreen Terrace"
        );
        patientUser.setId(1L);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(patientUser.getEmail(), null, patientUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UNIT: Get Current Patient Profile -> Returns Authenticated Profile")
    void testGetCurrentPatientProfileSuccess() {
        // Given
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When
        PatientResponseDto profile = patientService.getCurrentPatientProfile();

        // Then
        assertNotNull(profile);
        assertEquals(1L, profile.getId());
        assertEquals("Alex Morgan", profile.getFullName());
        assertEquals("alex@example.com", profile.getEmail());
        assertEquals("5552345678", profile.getPhone());
    }

    @Test
    @DisplayName("UNIT: Update Current Patient Profile -> Updates Demographic Fields")
    void testUpdateCurrentPatientProfileSuccess() {
        // Given
        PatientUpdateRequestDto updateDto = new PatientUpdateRequestDto(
                "Alex Morgan Updated",
                "5559990000",
                "AB+",
                "New Address Road"
        );

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PatientResponseDto updated = patientService.updateCurrentPatientProfile(updateDto);

        // Then
        assertNotNull(updated);
        assertEquals("Alex Morgan Updated", updated.getFullName());
        assertEquals("5559990000", updated.getPhone());
        assertEquals("AB+", updated.getBloodGroup());
        assertEquals("New Address Road", updated.getAddress());

        verify(userRepository, times(1)).save(patientUser);
    }

    @Test
    @DisplayName("UNIT: Patient Viewing Another Patient Profile -> Throws AccessDeniedException")
    void testGetAnotherPatientProfileThrowsAccessDenied() {
        // Given
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then (Patient with ID 1 attempts to query ID 2)
        assertThrows(
                AccessDeniedException.class,
                () -> patientService.getPatientById(2L)
        );

        verify(userRepository, never()).findById(2L);
    }

    @Test
    @DisplayName("UNIT: Patient Updating Another Patient Profile -> Throws AccessDeniedException")
    void testUpdateAnotherPatientProfileThrowsAccessDenied() {
        // Given
        PatientUpdateRequestDto updateDto = new PatientUpdateRequestDto("Tampered", "5551112222", "O+", "Address");
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then
        assertThrows(
                AccessDeniedException.class,
                () -> patientService.updatePatientById(2L, updateDto)
        );

        verify(userRepository, never()).findById(2L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Delete Current Patient Account -> Deletes User from Database")
    void testDeleteCurrentPatientAccountSuccess() {
        // Given
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When
        patientService.deleteCurrentPatientAccount();

        // Then
        verify(userRepository, times(1)).delete(patientUser);
    }

    @Test
    @DisplayName("UNIT: Patient Deleting Another Patient Account -> Throws AccessDeniedException")
    void testDeleteAnotherPatientAccountThrowsAccessDenied() {
        // Given
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then
        assertThrows(
                AccessDeniedException.class,
                () -> patientService.deletePatientAccountById(2L)
        );

        verify(userRepository, never()).delete(any());
    }
}
