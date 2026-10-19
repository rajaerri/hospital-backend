package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Appointment Spring Data JPA Repository
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findAllByOrderByAppointmentDateDescAppointmentTimeDesc();

    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :appointmentDate " +
           "AND a.appointmentTime = :appointmentTime " +
           "AND a.status != com.hospital.appointment.entity.AppointmentStatus.CANCELLED")
    boolean isDoctorSlotBooked(@Param("doctorId") Long doctorId,
                               @Param("appointmentDate") LocalDate appointmentDate,
                               @Param("appointmentTime") String appointmentTime);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
           "WHERE a.patient.id = :patientId " +
           "AND a.appointmentDate = :appointmentDate " +
           "AND a.appointmentTime = :appointmentTime " +
           "AND a.status != com.hospital.appointment.entity.AppointmentStatus.CANCELLED")
    boolean isPatientSlotBooked(@Param("patientId") Long patientId,
                                @Param("appointmentDate") LocalDate appointmentDate,
                                @Param("appointmentTime") String appointmentTime);
}
