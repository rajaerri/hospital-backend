package com.hospital.appointment.service;

import com.hospital.appointment.dto.PatientRequestDto;
import com.hospital.appointment.dto.PatientResponseDto;
import com.hospital.appointment.dto.PatientUpdateRequestDto;

import java.util.List;

/**
 * Patient Service Interface
 */
public interface PatientService {

    PatientResponseDto registerPatient(PatientRequestDto dto);

    List<PatientResponseDto> getAllPatients();

    PatientResponseDto getPatientById(Long id);

    PatientResponseDto getCurrentPatientProfile();

    PatientResponseDto updateCurrentPatientProfile(PatientUpdateRequestDto dto);

    PatientResponseDto updatePatientById(Long id, PatientUpdateRequestDto dto);

    void deleteCurrentPatientAccount();

    void deletePatientAccountById(Long id);
}
