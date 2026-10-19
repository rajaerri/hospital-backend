package com.hospital.appointment.controller;

import com.hospital.appointment.dto.ApiResponse;
import com.hospital.appointment.dto.PatientRequestDto;
import com.hospital.appointment.dto.PatientResponseDto;
import com.hospital.appointment.dto.PatientUpdateRequestDto;
import com.hospital.appointment.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient REST Controller
 * 
 * Endpoints:
 * 1. POST /api/patients        - Register a new patient
 * 2. GET  /api/patients        - Retrieve all registered patients
 * 3. GET  /api/patients/me     - Retrieve current authenticated patient's profile
 * 4. PUT  /api/patients/me     - Update current authenticated patient's profile
 * 5. GET  /api/patients/{id}   - Retrieve a single patient by ID (ownership checked)
 * 6. PUT  /api/patients/{id}   - Update a patient by ID (ownership checked)
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponseDto>> registerPatient(
            @Valid @RequestBody PatientRequestDto requestDto) {
        PatientResponseDto created = patientService.registerPatient(requestDto);
        ApiResponse<PatientResponseDto> response = ApiResponse.success(
                "Patient registered successfully in database.",
                created
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponseDto>>> getAllPatients() {
        List<PatientResponseDto> patients = patientService.getAllPatients();
        ApiResponse<List<PatientResponseDto>> response = ApiResponse.success(
                "Patients retrieved successfully.",
                patients
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponseDto>> getCurrentPatientProfile() {
        PatientResponseDto profile = patientService.getCurrentPatientProfile();
        ApiResponse<PatientResponseDto> response = ApiResponse.success(
                "Current patient profile retrieved successfully.",
                profile
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponseDto>> updateCurrentPatientProfile(
            @Valid @RequestBody PatientUpdateRequestDto updateDto) {
        PatientResponseDto updated = patientService.updateCurrentPatientProfile(updateDto);
        ApiResponse<PatientResponseDto> response = ApiResponse.success(
                "Patient profile updated successfully.",
                updated
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentPatientAccount() {
        patientService.deleteCurrentPatientAccount();
        ApiResponse<Void> response = ApiResponse.success(
                "Your account and all associated appointments have been deleted successfully.",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponseDto>> getPatientById(@PathVariable Long id) {
        PatientResponseDto patient = patientService.getPatientById(id);
        ApiResponse<PatientResponseDto> response = ApiResponse.success(
                "Patient profile retrieved successfully.",
                patient
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponseDto>> updatePatientById(
            @PathVariable Long id,
            @Valid @RequestBody PatientUpdateRequestDto updateDto) {
        PatientResponseDto updated = patientService.updatePatientById(id, updateDto);
        ApiResponse<PatientResponseDto> response = ApiResponse.success(
                "Patient #" + id + " profile updated successfully.",
                updated
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePatientAccountById(@PathVariable Long id) {
        patientService.deletePatientAccountById(id);
        ApiResponse<Void> response = ApiResponse.success(
                "Patient account #" + id + " deleted successfully.",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
