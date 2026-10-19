package com.hospital.appointment.controller;

import com.hospital.appointment.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health Controller
 * 
 * Provides system diagnostics and liveness health check.
 * Accessible at: GET http://localhost:8080/api/health
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name:hospital-appointment-system}")
    private String applicationName;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("application", applicationName);
        healthData.put("version", "1.0.0-SNAPSHOT");
        healthData.put("message", "Life Care Hospital Backend is running healthy and responsive!");
        healthData.put("timestamp", LocalDateTime.now().toString());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Health check passed successfully.",
                healthData
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
