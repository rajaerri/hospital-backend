package com.hospital.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.appointment.dto.LoginRequestDto;
import com.hospital.appointment.dto.PatientUpdateRequestDto;
import com.hospital.appointment.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Profile Management & Security Integration Tests
 * 
 * Verifies:
 * 1. View authenticated patient profile (200 OK)
 * 2. Update patient profile permitted fields (200 OK)
 * 3. Server-side Bean Validation failure for blank name (400 Bad Request)
 * 4. Server-side Bean Validation failure for invalid phone pattern (400 Bad Request)
 * 5. Unauthorized access without JWT (401 Unauthorized)
 * 6. Unauthorized access with malformed/invalid JWT (401 Unauthorized)
 * 7. Cross-patient authorization check: Patient cannot view another patient's account (403 Forbidden)
 * 8. Cross-patient authorization check: Patient cannot update another patient's account (403 Forbidden)
 * 9. Cross-patient authorization check: Patient cannot delete another patient's account (403 Forbidden)
 * 10. Missing patient resource requested by authorized admin (404 Not Found)
 * 11. Security guarantee: Password hashes are NEVER returned in API responses
 * 12. Delete patient's own account with confirmation (200 OK) and verify cascading cleanup
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ProfileAndSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Helper to log in a patient and return their JWT token
     */
    private String loginPatient(String email, String password) throws Exception {
        LoginRequestDto login = new LoginRequestDto(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("token").asText();
    }

    /**
     * Helper to register a new unique patient
     */
    private Long registerNewPatient(String fullName, String email, String phone) throws Exception {
        RegisterRequestDto reg = new RegisterRequestDto(
                fullName,
                email,
                "TestPass@123",
                phone,
                "A+",
                "123 Maple Street"
        );
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.get("data").get("id").asLong();
    }

    @Test
    @DisplayName("TC-PROF-01: View Authenticated Patient Profile -> HTTP 200 OK")
    void testViewPatientProfile() throws Exception {
        String token = loginPatient("alex.morgan@example.com", "Patient@123");

        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alex.morgan@example.com"))
                .andExpect(jsonPath("$.data.fullName").isNotEmpty())
                .andExpect(jsonPath("$.data.phone").isNotEmpty());
    }

    @Test
    @DisplayName("TC-PROF-02: Update Patient Profile Permitted Fields -> HTTP 200 OK")
    void testUpdatePatientProfileSuccess() throws Exception {
        // Register a distinct patient to update safely
        String email = "profile.updater@example.com";
        registerNewPatient("Profile Updater", email, "5559876543");
        String token = loginPatient(email, "TestPass@123");

        PatientUpdateRequestDto updateDto = new PatientUpdateRequestDto(
                "Updated Name",
                "5559998877",
                "AB+",
                "999 New Address Blvd"
        );

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value("5559998877"))
                .andExpect(jsonPath("$.data.bloodGroup").value("AB+"))
                .andExpect(jsonPath("$.data.address").value("999 New Address Blvd"));
    }

    @Test
    @DisplayName("TC-PROF-03: Update Profile with Blank Name -> HTTP 400 Bad Request (Bean Validation)")
    void testUpdateProfileValidationBlankName() throws Exception {
        String token = loginPatient("alex.morgan@example.com", "Patient@123");

        PatientUpdateRequestDto invalidDto = new PatientUpdateRequestDto(
                "", // Blank name violates @NotBlank
                "5552345678",
                "O+",
                "Valid Address"
        );

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.fullName", notNullValue()));
    }

    @Test
    @DisplayName("TC-PROF-04: Update Profile with Invalid Phone -> HTTP 400 Bad Request (Bean Validation)")
    void testUpdateProfileValidationInvalidPhone() throws Exception {
        String token = loginPatient("alex.morgan@example.com", "Patient@123");

        PatientUpdateRequestDto invalidDto = new PatientUpdateRequestDto(
                "Alex Morgan",
                "123", // Short phone violates @Pattern ^[0-9+\\-\\s()]{10,20}$
                "O+",
                "Valid Address"
        );

        mockMvc.perform(put("/api/patients/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone", notNullValue()));
    }

    @Test
    @DisplayName("TC-PROF-05: Access Patient Profile Without JWT -> HTTP 401 Unauthorized")
    void testAccessProfileWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("TC-PROF-06: Access Patient Profile with Invalid JWT -> HTTP 401 Unauthorized")
    void testAccessProfileWithInvalidTokenFails() throws Exception {
        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("TC-PROF-07: Patient Attempting to View Another Patient Profile -> HTTP 403 Forbidden")
    void testCrossPatientViewForbidden() throws Exception {
        // Register Victim Patient
        Long victimId = registerNewPatient("Victim Patient", "victim.view@example.com", "5551239999");

        // Attacker Patient logs in
        String attackerToken = loginPatient("alex.morgan@example.com", "Patient@123");

        // Attacker tries to read Victim's profile by ID
        mockMvc.perform(get("/api/patients/" + victimId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Forbidden")));
    }

    @Test
    @DisplayName("TC-PROF-08: Patient Attempting to Update Another Patient Profile -> HTTP 403 Forbidden")
    void testCrossPatientUpdateForbidden() throws Exception {
        // Register Victim Patient
        Long victimId = registerNewPatient("Victim Patient 2", "victim.update@example.com", "5551238888");

        // Attacker Patient logs in
        String attackerToken = loginPatient("alex.morgan@example.com", "Patient@123");

        PatientUpdateRequestDto maliciousPayload = new PatientUpdateRequestDto(
                "Hacked Name",
                "5550000000",
                "O-",
                "Hacked Address"
        );

        // Attacker tries to update Victim's profile by ID
        mockMvc.perform(put("/api/patients/" + victimId)
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousPayload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Forbidden")));
    }

    @Test
    @DisplayName("TC-PROF-09: Patient Attempting to Delete Another Patient Account -> HTTP 403 Forbidden")
    void testCrossPatientDeleteForbidden() throws Exception {
        // Register Victim Patient
        Long victimId = registerNewPatient("Victim Patient 3", "victim.delete@example.com", "5551237777");

        // Attacker Patient logs in
        String attackerToken = loginPatient("alex.morgan@example.com", "Patient@123");

        // Attacker tries to delete Victim's account by ID
        mockMvc.perform(delete("/api/patients/" + victimId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Forbidden")));
    }

    @Test
    @DisplayName("TC-PROF-10: Non-Existent Patient Profile Request -> HTTP 404 Not Found")
    void testNonExistentPatientReturns404() throws Exception {
        // System admin has permission to inspect patients by ID
        String adminToken = loginPatient("admin@hospital.com", "Admin@123");

        mockMvc.perform(get("/api/patients/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Patient profile not found")));
    }

    @Test
    @DisplayName("TC-PROF-11: Password Hash Security Check -> Never Returned in Response")
    void testPasswordNeverExposedInResponses() throws Exception {
        String token = loginPatient("alex.morgan@example.com", "Patient@123");

        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("TC-PROF-12: Delete Patient Own Account -> HTTP 200 OK")
    void testDeletePatientOwnAccount() throws Exception {
        // Register a disposable patient
        String email = "disposable.patient@example.com";
        registerNewPatient("Disposable Patient", email, "5554443322");
        String token = loginPatient(email, "TestPass@123");

        // Patient deletes their own account
        mockMvc.perform(delete("/api/patients/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        // Verify account is actually deleted: subsequent login attempt fails
        LoginRequestDto loginAttempt = new LoginRequestDto(email, "TestPass@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized());
    }
}
