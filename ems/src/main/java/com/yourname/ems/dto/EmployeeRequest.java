package com.yourname.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.yourname.ems.model.Department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload used to create or fully update an employee")
public record EmployeeRequest(

        @Schema(example = "John")
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
        @Pattern(regexp = "^[A-Za-z]*$", message = "First name must contain letters only")
        String firstName,

        @Schema(example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
        @Pattern(regexp = "^[A-Za-z]*$", message = "Last name must contain letters only")
        String lastName,

        @Schema(example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(example = "9876543210")
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a 10-digit number")
        String phone,

        @Schema(example = "IT", description = "One of HR, IT, FINANCE, SALES, OPERATIONS")
        @NotNull(message = "Department is required")
        Department department,

        @Schema(example = "Software Engineer")
        @NotBlank(message = "Designation is required")
        @Size(max = 60, message = "Designation must be at most 60 characters")
        String designation,

        @Schema(example = "75000.00")
        @NotNull(message = "Salary is required")
        @Positive(message = "Salary must be greater than 0")
        BigDecimal salary,

        @Schema(example = "2024-01-15", type = "string", format = "date")
        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining cannot be in the future")
        LocalDate dateOfJoining) {
}
