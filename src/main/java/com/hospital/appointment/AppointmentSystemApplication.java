package com.hospital.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Entry Point for Life Care Hospital Appointment System.
 * 
 * @SpringBootApplication encapsulates three key annotations:
 * 1. @Configuration: Tags the class as a source of bean definitions.
 * 2. @EnableAutoConfiguration: Tells Spring Boot to automatically configure Spring based on jar dependencies.
 * 3. @ComponentScan: Scans for other @Component, @Service, @Repository, and @Controller classes in this package.
 */
@SpringBootApplication
public class AppointmentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentSystemApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("  Life Care Hospital Backend Server Started Successfully!         ");
        System.out.println("  Health Check URL: http://localhost:8080/api/health              ");
        System.out.println("  Doctors API URL:  http://localhost:8080/api/doctors             ");
        System.out.println("==================================================================");
    }
}
