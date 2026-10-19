package com.hospital.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.appointment.dto.LoginRequestDto;
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
 * Full-Stack Authentication and Authorization Integration Tests
 * 
 * Verifies:
 * 1. BCrypt password hashing & registration (201 Created)
 * 2. Duplicate email rejection (400 Bad Request)
 * 3. Successful login issuing cryptographic JWT (200 OK)
 * 4. Failed login with invalid credentials (401 Unauthorized)
 * 5. Accessing protected endpoint without token (401 Unauthorized)
 * 6. Accessing protected endpoint with valid JWT (200 OK)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TC-01: Successful Patient Registration -> HTTP 201 Created")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequestDto registerDto = new RegisterRequestDto(
                "Jane Doe",
                "jane.doe@example.com",
                "SecurePass123",
                "5551112233",
                "B+",
                "123 Medical Way"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_PATIENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist()); // Security: Password is never in response
    }

    @Test
    @DisplayName("TC-02: Duplicate Email Registration -> HTTP 400 Bad Request")
    void testRegistrationWithDuplicateEmailFails() throws Exception {
        // alex.morgan@example.com is seeded on startup by DataInitializer
        RegisterRequestDto duplicateDto = new RegisterRequestDto(
                "Alex Copy",
                "alex.morgan@example.com",
                "AnotherPassword123",
                "5552223344",
                "O+",
                "Springfield"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("TC-03: Successful Login -> HTTP 200 OK with Valid JWT Token")
    void testSuccessfulLoginReturnsJwt() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto("alex.morgan@example.com", "Patient@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("alex.morgan@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_PATIENT"));
    }

    @Test
    @DisplayName("TC-04: Failed Login with Wrong Password -> HTTP 401 Unauthorized")
    void testLoginWithWrongPasswordFails() throws Exception {
        LoginRequestDto badLogin = new LoginRequestDto("alex.morgan@example.com", "WrongPassword999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Authentication Failed")));
    }

    @Test
    @DisplayName("TC-05: Access Protected Endpoint Without Token -> HTTP 401 Unauthorized")
    void testProtectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("TC-06: Access Protected Endpoint With Valid JWT -> HTTP 200 OK")
    void testProtectedEndpointWithValidTokenReturns200() throws Exception {
        // Step 1: Login to obtain real JWT
        LoginRequestDto loginDto = new LoginRequestDto("alex.morgan@example.com", "Patient@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseNode.get("data").get("token").asText();

        // Step 2: Use Bearer token to request protected appointments endpoint
        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(Iterable.class)));
    }
}
