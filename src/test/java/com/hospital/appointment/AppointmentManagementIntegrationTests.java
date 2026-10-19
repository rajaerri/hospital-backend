package com.hospital.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.appointment.dto.AppointmentRequestDto;
import com.hospital.appointment.dto.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Appointment Management Integration Tests
 * 
 * Verifies:
 * 1. Booking an appointment with selected doctor, date, and time (201 Created)
 * 2. Rejecting bookings with past dates (400 Bad Request)
 * 3. Preventing duplicate bookings for the same doctor and time slot (400 Bad Request)
 * 4. Listing authenticated patient's appointments (200 OK)
 * 5. Cancelling an appointment (200 OK, status CANCELLED)
 * 6. Preventing repeated cancellation of already cancelled appointments (400 Bad Request)
 * 7. Preventing patient from viewing another patient's appointments (403 Forbidden)
 * 8. Rejecting unauthenticated booking requests (401 Unauthorized)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AppointmentManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginPatient() throws Exception {
        LoginRequestDto login = new LoginRequestDto("alex.morgan@example.com", "Patient@123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("token").asText();
    }

    @Test
    @DisplayName("TC-APT-01: Successful Appointment Booking -> HTTP 201 Created")
    void testSuccessfulBooking() throws Exception {
        String token = loginPatient();
        LocalDate futureDate = LocalDate.now().plusDays(5);

        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                futureDate,
                "09:00 AM",
                "Routine cardiac evaluation"
        );

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.doctorId").value(1))
                .andExpect(jsonPath("$.data.doctorName", containsString("Sarah Jenkins")))
                .andExpect(jsonPath("$.data.patientName").value("Alex Morgan"))
                .andExpect(jsonPath("$.data.appointmentTime").value("09:00 AM"))
                .andExpect(jsonPath("$.data.status").value("BOOKED"));
    }

    @Test
    @DisplayName("TC-APT-02: Past Date Validation -> HTTP 400 Bad Request")
    void testBookingWithPastDateFails() throws Exception {
        String token = loginPatient();
        LocalDate pastDate = LocalDate.now().minusDays(3);

        AppointmentRequestDto request = new AppointmentRequestDto(
                2L,
                pastDate,
                "10:00 AM",
                "Past checkup"
        );

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("TC-APT-03: Duplicate Booking for Same Doctor & Slot -> HTTP 400 Bad Request")
    void testDuplicateBookingFails() throws Exception {
        String token = loginPatient();
        LocalDate futureDate = LocalDate.now().plusDays(10);

        AppointmentRequestDto request = new AppointmentRequestDto(
                3L,
                futureDate,
                "11:00 AM",
                "Pediatric consult"
        );

        // 1. First booking succeeds
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Second booking for identical doctor, date, and slot must fail
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already booked")));
    }

    @Test
    @DisplayName("TC-APT-04: View Patient Appointments -> HTTP 200 OK")
    void testViewPatientAppointments() throws Exception {
        String token = loginPatient();

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-APT-05: Cancel Appointment -> HTTP 200 OK and Status CANCELLED")
    void testCancelAppointment() throws Exception {
        String token = loginPatient();
        LocalDate futureDate = LocalDate.now().plusDays(14);

        // Book an appointment first
        AppointmentRequestDto request = new AppointmentRequestDto(
                4L,
                futureDate,
                "02:00 PM",
                "Orthopedic knee consultation"
        );

        MvcResult bookResult = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(bookResult.getResponse().getContentAsString());
        long appointmentId = root.get("data").get("id").asLong();

        // Cancel the appointment
        mockMvc.perform(put("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("TC-APT-06: Repeated Cancellation of Already Cancelled Slot -> HTTP 400 Bad Request")
    void testCancellingAlreadyCancelledFails() throws Exception {
        String token = loginPatient();
        LocalDate futureDate = LocalDate.now().plusDays(18);

        AppointmentRequestDto request = new AppointmentRequestDto(
                5L,
                futureDate,
                "03:00 PM",
                "Internal medicine review"
        );

        MvcResult bookResult = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        long appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // First cancellation
        mockMvc.perform(put("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Second cancellation must fail
        mockMvc.perform(put("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already cancelled")));
    }

    @Test
    @DisplayName("TC-APT-07: Patient Accessing Another Patient's Records -> HTTP 403 Forbidden")
    void testPatientCannotAccessAnotherPatientAppointments() throws Exception {
        String token = loginPatient();

        // Alex Morgan (Patient ID 2) tries to access Patient ID 999
        mockMvc.perform(get("/api/appointments/patient/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Forbidden")));
    }

    @Test
    @DisplayName("TC-APT-08: Unauthenticated Access Without JWT -> HTTP 401 Unauthorized")
    void testUnauthenticatedBookingFails() throws Exception {
        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                LocalDate.now().plusDays(1),
                "10:00 AM",
                "Unauthenticated attempt"
        );

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
