package com.hospital.appointment.service;

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
import com.hospital.appointment.service.impl.AppointmentServiceImpl;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Beginner-Friendly Unit Tests for AppointmentService
 * 
 * Verifies business logic rules in complete isolation:
 * 1. Future appointment date requirement (cannot book past dates)
 * 2. Doctor existence verification
 * 3. Prevention of doctor time slot double-booking
 * 4. Prevention of patient time slot conflicts
 * 5. Successful booking creation
 * 6. Safe cancellation and prevention of re-cancelling already cancelled appointments
 * 7. Ownership security: patients cannot cancel other patients' appointments
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceUnitTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private User patientUser;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patientUser = new User(
                "Alex Morgan",
                "alex@example.com",
                "$2a$10$hash",
                "5552345678",
                Role.ROLE_PATIENT,
                "O+",
                "Springfield"
        );
        patientUser.setId(1L);

        doctor = new Doctor(
                "Dr. Sarah Jenkins",
                "Cardiology",
                "MBBS, MD",
                "12 years",
                "Room 204",
                "$50",
                "Mon, Wed, Fri",
                4.9
        );
        doctor.setId(10L);

        // Configure Spring Security Context with the test patient
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(patientUser.getEmail(), null, patientUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UNIT: Successful Appointment Booking -> Status BOOKED")
    void testCreateAppointmentSuccess() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(2);
        String timeSlot = "10:00 AM";

        AppointmentRequestDto request = new AppointmentRequestDto(10L, futureDate, timeSlot, "Regular Checkup");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.isDoctorSlotBooked(10L, futureDate, timeSlot)).thenReturn(false);
        when(appointmentRepository.isPatientSlotBooked(1L, futureDate, timeSlot)).thenReturn(false);

        Appointment savedAppointment = new Appointment(
                patientUser,
                doctor,
                futureDate,
                timeSlot,
                AppointmentStatus.BOOKED,
                "Regular Checkup"
        );
        savedAppointment.setId(100L);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // When
        AppointmentResponseDto response = appointmentService.createAppointment(request);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Alex Morgan", response.getPatientName());
        assertEquals("Dr. Sarah Jenkins", response.getDoctorName());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("UNIT: Booking Past Date -> Throws IllegalArgumentException")
    void testCreateAppointmentPastDateThrowsException() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);
        AppointmentRequestDto request = new AppointmentRequestDto(10L, pastDate, "10:00 AM", "Checkup");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(ex.getMessage().contains("cannot be in the past"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Booking with Non-Existent Doctor -> Throws ResourceNotFoundException")
    void testCreateAppointmentDoctorNotFoundThrowsException() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(1);
        AppointmentRequestDto request = new AppointmentRequestDto(999L, futureDate, "10:00 AM", "Checkup");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.createAppointment(request)
        );

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Doctor Slot Double-Booking -> Throws IllegalArgumentException")
    void testCreateAppointmentDoctorSlotConflictThrowsException() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(2);
        String timeSlot = "10:00 AM";
        AppointmentRequestDto request = new AppointmentRequestDto(10L, futureDate, timeSlot, "Checkup");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.isDoctorSlotBooked(10L, futureDate, timeSlot)).thenReturn(true);

        // When & Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(ex.getMessage().contains("already booked"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Patient Double-Booking at Same Time -> Throws IllegalArgumentException")
    void testCreateAppointmentPatientSlotConflictThrowsException() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(2);
        String timeSlot = "10:00 AM";
        AppointmentRequestDto request = new AppointmentRequestDto(10L, futureDate, timeSlot, "Checkup");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.isDoctorSlotBooked(10L, futureDate, timeSlot)).thenReturn(false);
        when(appointmentRepository.isPatientSlotBooked(1L, futureDate, timeSlot)).thenReturn(true);

        // When & Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(ex.getMessage().contains("already have an active appointment"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Successful Appointment Cancellation -> Status Becomes CANCELLED")
    void testCancelAppointmentSuccess() {
        // Given
        Appointment appointment = new Appointment(
                patientUser,
                doctor,
                LocalDate.now().plusDays(3),
                "02:00 PM",
                AppointmentStatus.BOOKED,
                "Consultation"
        );
        appointment.setId(50L);

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AppointmentResponseDto response = appointmentService.cancelAppointment(50L);

        // Then
        assertNotNull(response);
        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
        verify(appointmentRepository, times(1)).save(appointment);
    }

    @Test
    @DisplayName("UNIT: Cancelling Already Cancelled Appointment -> Throws IllegalStateException")
    void testCancelAlreadyCancelledAppointmentThrowsException() {
        // Given
        Appointment appointment = new Appointment(
                patientUser,
                doctor,
                LocalDate.now().plusDays(3),
                "02:00 PM",
                AppointmentStatus.CANCELLED,
                "Consultation"
        );
        appointment.setId(50L);

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> appointmentService.cancelAppointment(50L)
        );

        assertTrue(ex.getMessage().contains("already cancelled"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNIT: Cancelling Another Patient's Appointment -> Throws AccessDeniedException")
    void testCancelAnotherPatientAppointmentThrowsAccessDenied() {
        // Given
        User anotherPatient = new User(
                "Other Patient",
                "other@example.com",
                "$2a$10$hash",
                "5550009999",
                Role.ROLE_PATIENT,
                "B+",
                "Elsewhere"
        );
        anotherPatient.setId(99L); // Different ID from patientUser (ID = 1L)

        Appointment appointment = new Appointment(
                anotherPatient,
                doctor,
                LocalDate.now().plusDays(3),
                "02:00 PM",
                AppointmentStatus.BOOKED,
                "Consultation"
        );
        appointment.setId(50L);

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(patientUser));

        // When & Then
        assertThrows(
                AccessDeniedException.class,
                () -> appointmentService.cancelAppointment(50L)
        );

        verify(appointmentRepository, never()).save(any());
    }
}
