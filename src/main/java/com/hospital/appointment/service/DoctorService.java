package com.hospital.appointment.service;

import com.hospital.appointment.dto.DoctorResponseDto;

import java.util.List;

/**
 * Doctor Service Interface
 * 
 * Defines business operations for querying and searching hospital doctors.
 */
public interface DoctorService {

    List<DoctorResponseDto> getDoctors(String search, String specialty);

    List<DoctorResponseDto> getAllDoctors();

    DoctorResponseDto getDoctorById(Long id);

    List<DoctorResponseDto> getDoctorsBySpecialty(String specialty);
}
