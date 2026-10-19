package com.hospital.appointment.service.impl;

import com.hospital.appointment.dto.AppointmentRequestDto;
import com.hospital.appointment.dto.AppointmentResponseDto;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.AppointmentStatus;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.UserRepository;
import com.hospital.appointment.service.AppointmentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Appointment Service Implementation
 * 
 * Enforces business logic and security policies:
 * - Patient identity is derived directly from the authenticated JWT session.
 * - Validates appointment date is not in the past.
 * - Prevents duplicate bookings for the same doctor and time slot.
 * - Checks availability on the server.
 * - Ensures patients cannot view or cancel another patient's appointments.
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  UserRepository userRepository,
                                  DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Full authentication is required to access appointment services.");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found for: " + email));
    }

    @Override
    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
        User currentUser = getAuthenticatedUser();

        // 1. Obtain patient identity from authenticated user (not from client-submitted patientId)
        User patient;
        if (currentUser.getRole() == Role.ROLE_ADMIN && dto.getPatientId() != null) {
            patient = userRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + dto.getPatientId()));
        } else {
            patient = currentUser;
        }

        // 2. Validate appointment date is not in the past
        if (dto.getAppointmentDate() == null || dto.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date cannot be in the past.");
        }

        // 3. Validate Doctor exists
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + dto.getDoctorId()));

        // 4. Server-side availability check: Prevent duplicate booking for the same doctor and time slot
        boolean isDoctorBooked = appointmentRepository.isDoctorSlotBooked(
                doctor.getId(),
                dto.getAppointmentDate(),
                dto.getAppointmentTime()
        );
        if (isDoctorBooked) {
            throw new IllegalArgumentException(
                    "Doctor " + doctor.getName() + " is already booked for " +
                    dto.getAppointmentDate() + " at " + dto.getAppointmentTime() +
                    ". Please select another time slot."
            );
        }

        // 5. Prevent double-booking for the same patient at the exact same time
        boolean isPatientBooked = appointmentRepository.isPatientSlotBooked(
                patient.getId(),
                dto.getAppointmentDate(),
                dto.getAppointmentTime()
        );
        if (isPatientBooked) {
            throw new IllegalArgumentException(
                    "You already have an active appointment scheduled on " +
                    dto.getAppointmentDate() + " at " + dto.getAppointmentTime() + "."
            );
        }

        // 6. Save appointment with BOOKED status
        Appointment appointment = new Appointment(
                patient,
                doctor,
                dto.getAppointmentDate(),
                dto.getAppointmentTime(),
                AppointmentStatus.BOOKED,
                dto.getReason()
        );

        Appointment saved = appointmentRepository.save(appointment);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAllAppointments() {
        User currentUser = getAuthenticatedUser();

        // If authenticated as PATIENT, return only their own appointments
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(currentUser.getId())
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        // Admins can see all appointments
        return appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAppointmentsByPatient(Long patientId) {
        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only view their own appointments
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(patientId)) {
                throw new AccessDeniedException("Access Denied: You are not authorized to view appointments for another patient.");
            }
        }

        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }

        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only view their own appointment
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(appointment.getPatient().getId())) {
                throw new AccessDeniedException("Access Denied: You are not authorized to view this appointment.");
            }
        }

        return mapToDto(appointment);
    }

    @Override
    public AppointmentResponseDto cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        User currentUser = getAuthenticatedUser();

        // Ownership Check: Patient can only cancel their own appointment
        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            if (!currentUser.getId().equals(appointment.getPatient().getId())) {
                throw new AccessDeniedException("Access Denied: You cannot cancel an appointment belonging to another patient.");
            }
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment #" + appointmentId + " is already cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        return mapToDto(updated);
    }

    private AppointmentResponseDto mapToDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName(),
                appointment.getDoctor().getSpecialty(),
                appointment.getDoctor().getRoomNo(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getReason()
        );
    }
}
