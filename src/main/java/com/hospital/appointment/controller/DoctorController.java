package com.hospital.appointment.controller;

import com.hospital.appointment.dto.ApiResponse;
import com.hospital.appointment.dto.DoctorResponseDto;
import com.hospital.appointment.service.DoctorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor REST Controller
 * 
 * Endpoints:
 * 1. GET /api/doctors                          - Retrieve all doctors (with optional search and specialty filters)
 * 2. GET /api/doctors/{id}                     - Retrieve a specific doctor by ID
 * 3. GET /api/doctors/department/{specialty}   - Filter doctors by department
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    // Constructor Dependency Injection
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Retrieve doctors with optional search and specialty query params.
     * Examples:
     * - GET /api/doctors
     * - GET /api/doctors?search=Sarah
     * - GET /api/doctors?specialty=Cardiology
     * - GET /api/doctors?search=Chen&specialty=Neurology
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponseDto>>> getDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialty) {
        List<DoctorResponseDto> doctors = doctorService.getDoctors(search, specialty);
        ApiResponse<List<DoctorResponseDto>> response = ApiResponse.success(
                "Doctors retrieved successfully.",
                doctors
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieve a single doctor by ID.
     * Response: HTTP 200 OK if found, or HTTP 404 NOT FOUND if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponseDto>> getDoctorById(@PathVariable Long id) {
        DoctorResponseDto doctor = doctorService.getDoctorById(id);
        ApiResponse<DoctorResponseDto> response = ApiResponse.success(
                "Doctor details retrieved successfully.",
                doctor
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filter doctors by department / specialty directly in path.
     * Example: GET /api/doctors/department/Cardiology
     */
    @GetMapping("/department/{specialty}")
    public ResponseEntity<ApiResponse<List<DoctorResponseDto>>> getDoctorsByDepartment(
            @PathVariable String specialty) {
        List<DoctorResponseDto> doctors = doctorService.getDoctorsBySpecialty(specialty);
        ApiResponse<List<DoctorResponseDto>> response = ApiResponse.success(
                "Doctors in specialty '" + specialty + "' retrieved successfully.",
                doctors
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
