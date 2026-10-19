package com.hospital.appointment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Doctor Directory REST Endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class DoctorDirectoryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DOC-01: GET /api/doctors -> Returns HTTP 200 and all seeded doctors")
    void testGetAllDoctors() throws Exception {
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.data[0].name", notNullValue()))
                .andExpect(jsonPath("$.data[0].specialty", notNullValue()))
                .andExpect(jsonPath("$.data[0].fee", notNullValue()));
    }

    @Test
    @DisplayName("DOC-02: GET /api/doctors/{id} -> Returns HTTP 200 and doctor details")
    void testGetDoctorById() throws Exception {
        mockMvc.perform(get("/api/doctors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Dr. Sarah Jenkins"))
                .andExpect(jsonPath("$.data.specialty").value("Cardiology"));
    }

    @Test
    @DisplayName("DOC-03: GET /api/doctors/999 -> Returns HTTP 404 Not Found for invalid ID")
    void testGetDoctorByInvalidIdReturns404() throws Exception {
        mockMvc.perform(get("/api/doctors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Doctor not found with ID: 999")));
    }

    @Test
    @DisplayName("DOC-04: GET /api/doctors?search=Sarah -> Returns matching doctor")
    void testSearchDoctorByName() throws Exception {
        mockMvc.perform(get("/api/doctors").param("search", "Sarah"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Dr. Sarah Jenkins"));
    }

    @Test
    @DisplayName("DOC-05: GET /api/doctors?specialty=Neurology -> Returns matching specialty doctors")
    void testFilterDoctorsBySpecialty() throws Exception {
        mockMvc.perform(get("/api/doctors").param("specialty", "Neurology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].specialty").value("Neurology"));
    }

    @Test
    @DisplayName("DOC-06: GET /api/doctors?search=NonExistent -> Returns empty list")
    void testSearchWithNoResultsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/doctors").param("search", "NonExistentXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
