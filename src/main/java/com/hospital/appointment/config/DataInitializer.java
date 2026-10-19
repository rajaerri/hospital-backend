package com.hospital.appointment.config;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.AppointmentStatus;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Data Initializer
 * 
 * Runs once upon application startup:
 * 1. Seeds default fictional doctors
 * 2. Seeds an ADMIN user (admin@hospital.com / Admin@123) with BCrypt hash
 * 3. Seeds a demo PATIENT user (alex.morgan@example.com / Patient@123) with BCrypt hash
 * 4. Seeds an initial confirmed appointment
 */
@Configuration
public class DataInitializer implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(DoctorRepository doctorRepository,
                           UserRepository userRepository,
                           AppointmentRepository appointmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Doctors if empty
        if (doctorRepository.count() == 0) {
            System.out.println("--> Seeding fictional medical doctors into MySQL...");
            List<Doctor> sampleDoctors = Arrays.asList(
                    new Doctor("Dr. Sarah Jenkins", "Cardiology", "MBBS, MD (Cardiology)",
                            "12 years", "Room 204, Block A", "$50", "Mon, Wed, Fri", 4.9),
                    new Doctor("Dr. Robert Chen", "Neurology", "MBBS, DM (Neurology)",
                            "15 years", "Room 310, Block B", "$65", "Tue, Thu, Sat", 4.8),
                    new Doctor("Dr. Emily Watson", "Pediatrics", "MBBS, DCH, MD (Pediatrics)",
                            "8 years", "Room 105, Child Care Wing", "$40", "Mon to Sat", 5.0),
                    new Doctor("Dr. Michael Adams", "Orthopedics", "MBBS, MS (Orthopedics)",
                            "14 years", "Room 215, Block A", "$55", "Mon, Wed, Thu", 4.7),
                    new Doctor("Dr. Priya Patel", "General Medicine", "MBBS, MD (Internal Medicine)",
                            "10 years", "Room 102, OPD Clinic", "$35", "Mon to Fri", 4.9),
                    new Doctor("Dr. David Miller", "Dermatology", "MBBS, MD (Dermatology)",
                            "9 years", "Room 304, Block C", "$45", "Tue, Fri, Sat", 4.8)
            );
            doctorRepository.saveAll(sampleDoctors);
            System.out.println("--> Successfully seeded " + sampleDoctors.size() + " doctors.");
        }

        // 2. Seed Default Admin User if not exists
        if (!userRepository.existsByEmail("admin@hospital.com")) {
            System.out.println("--> Seeding default system administrator...");
            User admin = new User(
                    "System Administrator",
                    "admin@hospital.com",
                    passwordEncoder.encode("Admin@123"), // BCrypt Hashed
                    "5550001111",
                    Role.ROLE_ADMIN,
                    "O+",
                    "Hospital Headquarters, Suite 100"
            );
            userRepository.save(admin);
        }

        // 3. Seed Default Demo Patient User if not exists
        if (!userRepository.existsByEmail("alex.morgan@example.com")) {
            System.out.println("--> Seeding demo patient user...");
            User patient = new User(
                    "Alex Morgan",
                    "alex.morgan@example.com",
                    passwordEncoder.encode("Patient@123"), // BCrypt Hashed
                    "5552345678",
                    Role.ROLE_PATIENT,
                    "O+",
                    "742 Evergreen Terrace, Springfield"
            );
            userRepository.save(patient);

            // 4. Seed an Initial Appointment for the Demo Patient
            Doctor firstDoctor = doctorRepository.findAll().stream().findFirst().orElse(null);
            if (firstDoctor != null) {
                Appointment appointment = new Appointment(
                        patient,
                        firstDoctor,
                        LocalDate.now().plusDays(3),
                        "10:30 AM",
                        AppointmentStatus.CONFIRMED,
                        "Annual Preventive Heart Screening"
                );
                appointmentRepository.save(appointment);
                System.out.println("--> Seeded initial appointment for demo patient Alex Morgan.");
            }
        }
    }
}
