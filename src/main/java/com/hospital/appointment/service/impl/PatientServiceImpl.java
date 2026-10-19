package com.hospital.appointment.service.impl;

import com.hospital.appointment.dto.PatientRequestDto;
import com.hospital.appointment.dto.PatientResponseDto;
import com.hospital.appointment.dto.PatientUpdateRequestDto;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.UserRepository;
import com.hospital.appointment.service.PatientService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Patient Service Implementation backed by MySQL users table.
 * Enforces ownership checks: Patients can ONLY view, edit, or delete their OWN records.
 */
@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Full authentication is required to access patient services.");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found for: " + email));
    }

    @Override
    public PatientResponseDto registerPatient(PatientRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("A patient with email '" + dto.getEmail() + "' already exists.");
        }

        String defaultHashedPassword = passwordEncoder.encode("Patient@123");

        User user = new User(
                dto.getFullName().trim(),
                dto.getEmail().trim().toLowerCase(),
                defaultHashedPassword,
                dto.getPhone().trim(),
                Role.ROLE_PATIENT,
                dto.getBloodGroup(),
                dto.getAddress()
        );

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponseDto> getAllPatients() {
        User currentUser = getAuthenticatedUser();

        // Patients only see themselves in directory
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            return List.of(mapToDto(currentUser));
        }

        // Admins can see all registered patients
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_PATIENT)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDto getPatientById(Long id) {
        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only view their own profile
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(id)) {
                throw new AccessDeniedException("Access Denied: You are not authorized to view another patient's profile.");
            }
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + id));

        return mapToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDto getCurrentPatientProfile() {
        User currentUser = getAuthenticatedUser();
        return mapToDto(currentUser);
    }

    @Override
    public PatientResponseDto updateCurrentPatientProfile(PatientUpdateRequestDto dto) {
        User currentUser = getAuthenticatedUser();
        applyUpdates(currentUser, dto);
        User saved = userRepository.save(currentUser);
        return mapToDto(saved);
    }

    @Override
    public PatientResponseDto updatePatientById(Long id, PatientUpdateRequestDto dto) {
        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only update their own profile
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(id)) {
                throw new AccessDeniedException("Access Denied: You are not authorized to update another patient's profile.");
            }
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + id));

        applyUpdates(user, dto);
        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    @Override
    public void deleteCurrentPatientAccount() {
        User currentUser = getAuthenticatedUser();
        // Automatically cascades and deletes appointments via orphanRemoval = true
        userRepository.delete(currentUser);
    }

    @Override
    public void deletePatientAccountById(Long id) {
        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only delete their own account
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(id)) {
                throw new AccessDeniedException("Access Denied: You cannot delete another patient's account.");
            }
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found with ID: " + id));

        userRepository.delete(user);
    }

    private void applyUpdates(User user, PatientUpdateRequestDto dto) {
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName().trim());
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getBloodGroup() != null) {
            user.setBloodGroup(dto.getBloodGroup().trim());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress().trim());
        }
    }

    private PatientResponseDto mapToDto(User user) {
        return new PatientResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getBloodGroup(),
                user.getAddress()
        );
    }
}
