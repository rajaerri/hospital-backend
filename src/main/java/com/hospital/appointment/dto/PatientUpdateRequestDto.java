package com.hospital.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Patient Profile Update Request DTO
 * 
 * Enforces server-side validation rules:
 * - Full name must be between 3 and 100 characters
 * - Phone number must be 10-20 digits
 * - Blood group format validation
 * - Address length boundary
 */
public class PatientUpdateRequestDto {

    @NotBlank(message = "Full name cannot be blank")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{10,20}$", message = "Phone number must contain at least 10 valid digits")
    private String phone;

    @Pattern(regexp = "^(A\\+|A\\-|B\\+|B\\-|AB\\+|AB\\-|O\\+|O\\-)?$", message = "Invalid blood group format (e.g. A+, B+, O+, AB-)")
    private String bloodGroup;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public PatientUpdateRequestDto() {
    }

    public PatientUpdateRequestDto(String fullName, String phone, String bloodGroup, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.address = address;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
