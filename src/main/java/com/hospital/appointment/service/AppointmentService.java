package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentRequestDto;
import com.hospital.appointment.dto.AppointmentResponseDto;

import java.util.List;

/**
 * Appointment Service Interface
 */
public interface AppointmentService {

    AppointmentResponseDto createAppointment(AppointmentRequestDto dto);

    List<AppointmentResponseDto> getAllAppointments();

    List<AppointmentResponseDto> getAppointmentsByPatient(Long patientId);

    AppointmentResponseDto getAppointmentById(Long appointmentId);

    AppointmentResponseDto cancelAppointment(Long appointmentId);
}
