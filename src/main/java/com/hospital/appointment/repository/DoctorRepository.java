package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Doctor Spring Data JPA Repository
 * 
 * Provides database queries for:
 * 1. Standard CRUD operations via JpaRepository
 * 2. Specialty-based lookup
 * 3. Case-insensitive search by doctor name or medical specialty
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    @Query("SELECT d FROM Doctor d WHERE " +
           "(:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.specialty) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Doctor> searchDoctors(@Param("search") String search);
}
