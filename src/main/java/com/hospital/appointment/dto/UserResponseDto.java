package com.hospital.appointment.dto;

import com.hospital.appointment.entity.Role;

/**
 * Public User Response DTO
 * 
 * Never includes passwords or security tokens.
 */
public class UserResponseDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String bloodGroup;
    private String address;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String fullName, String email, String phone,
                           Role role, String bloodGroup, String address) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.bloodGroup = bloodGroup;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
