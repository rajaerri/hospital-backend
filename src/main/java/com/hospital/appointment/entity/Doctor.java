package com.hospital.appointment.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Doctor Entity
 * 
 * Maps directly to the 'doctors' table in MySQL via Spring Data JPA and Hibernate.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(length = 150)
    private String qualification;

    @Column(length = 50)
    private String experience;

    @Column(length = 100)
    private String roomNo;

    @Column(length = 20)
    private String fee;

    @Column(length = 100)
    private String availableDays;

    private Double rating;

    // Relationship: One Doctor can have Many Appointments
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments = new ArrayList<>();

    public Doctor() {
    }

    public Doctor(String name, String specialty, String qualification, String experience,
                  String roomNo, String fee, String availableDays, Double rating) {
        this.name = name;
        this.specialty = specialty;
        this.qualification = qualification;
        this.experience = experience;
        this.roomNo = roomNo;
        this.fee = fee;
        this.availableDays = availableDays;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
}
