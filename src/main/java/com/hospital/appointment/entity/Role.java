package com.hospital.appointment.entity;

/**
 * System Role Enum
 * 
 * ROLE_PATIENT: Can manage their own profile and appointments.
 * ROLE_ADMIN:   Can manage all patients, doctors, and appointments.
 */
public enum Role {
    ROLE_PATIENT,
    ROLE_ADMIN
}
