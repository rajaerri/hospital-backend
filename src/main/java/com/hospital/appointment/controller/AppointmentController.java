package com.hospital.appointment.controller;

import com.hospital.appointment.dto.ApiResponse;
import com.hospital.appointment.dto.AppointmentRequestDto;
import com.hospital.appointment.dto.AppointmentResponseDto;
import com.hospital.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Appointment REST Controller
 * 
 * Endpoints:
 * 1. POST /api/appointments                     - Book a new appointment (patient derived from JWT)
 * 2. GET  /api/appointments                     - Retrieve patient's appointments (or all for admin)
 * 3. GET  /api/appointments/{id}                - Retrieve single appointment details (with ownership check)
 * 4. GET  /api/appointments/patient/{patientId} - Retrieve appointments for a patient (with ownership check)
 * 5. PUT  /api/appointments/{id}/cancel         - Cancel an appointment (with ownership check)
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> createAppointment(
            @Valid @RequestBody AppointmentRequestDto requestDto) {
        AppointmentResponseDto created = appointmentService.createAppointment(requestDto);
        ApiResponse<AppointmentResponseDto> response = ApiResponse.success(
                "Appointment successfully booked.",
                created
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponseDto>>> getAllAppointments() {
        List<AppointmentResponseDto> appointments = appointmentService.getAllAppointments();
        ApiResponse<List<AppointmentResponseDto>> response = ApiResponse.success(
                "Appointments retrieved successfully.",
                appointments
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> getAppointmentById(@PathVariable Long id) {
        AppointmentResponseDto appointment = appointmentService.getAppointmentById(id);
        ApiResponse<AppointmentResponseDto> response = ApiResponse.success(
                "Appointment details retrieved successfully.",
                appointment
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDto>>> getAppointmentsByPatient(
            @PathVariable Long patientId) {
        List<AppointmentResponseDto> appointments = appointmentService.getAppointmentsByPatient(patientId);
        ApiResponse<List<AppointmentResponseDto>> response = ApiResponse.success(
                "Appointments for patient #" + patientId + " retrieved successfully.",
                appointments
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> cancelAppointment(@PathVariable Long id) {
        AppointmentResponseDto cancelled = appointmentService.cancelAppointment(id);
        ApiResponse<AppointmentResponseDto> response = ApiResponse.success(
                "Appointment #" + id + " has been successfully cancelled.",
                cancelled
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
