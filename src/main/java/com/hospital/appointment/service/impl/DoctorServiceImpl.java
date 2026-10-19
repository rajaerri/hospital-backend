package com.hospital.appointment.service.impl;

import com.hospital.appointment.dto.DoctorResponseDto;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.exception.ResourceNotFoundException;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.service.DoctorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Doctor Service Implementation backed by MySQL & Spring Data JPA.
 */
@Service
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public List<DoctorResponseDto> getDoctors(String search, String specialty) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasSpecialty = specialty != null && !specialty.trim().isEmpty() && !specialty.equalsIgnoreCase("All");

        List<Doctor> results;

        if (hasSearch) {
            String cleanSearch = search.trim();
            results = doctorRepository.searchDoctors(cleanSearch);
            if (hasSpecialty) {
                results = results.stream()
                        .filter(d -> d.getSpecialty().equalsIgnoreCase(specialty.trim()))
                        .collect(Collectors.toList());
            }
        } else if (hasSpecialty) {
            results = doctorRepository.findBySpecialtyIgnoreCase(specialty.trim());
        } else {
            results = doctorRepository.findAll();
        }

        return results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoctorResponseDto> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorResponseDto getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
        return mapToDto(doctor);
    }

    @Override
    public List<DoctorResponseDto> getDoctorsBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyIgnoreCase(specialty).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private DoctorResponseDto mapToDto(Doctor doctor) {
        return new DoctorResponseDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialty(),
                doctor.getQualification(),
                doctor.getExperience(),
                doctor.getRoomNo(),
                doctor.getFee(),
                doctor.getAvailableDays(),
                doctor.getRating()
        );
    }
}
