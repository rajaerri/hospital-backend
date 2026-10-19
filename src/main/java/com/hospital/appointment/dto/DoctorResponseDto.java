package com.hospital.appointment.dto;

/**
 * Doctor Response DTO (Data Transfer Object)
 * 
 * Separates internal database entity structure from the public REST API response.
 */
public class DoctorResponseDto {

    private Long id;
    private String name;
    private String specialty;
    private String qualification;
    private String experience;
    private String roomNo;
    private String fee;
    private String availableDays;
    private Double rating;

    public DoctorResponseDto() {
    }

    public DoctorResponseDto(Long id, String name, String specialty, String qualification,
                             String experience, String roomNo, String fee, String availableDays, Double rating) {
        this.id = id;
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
}
