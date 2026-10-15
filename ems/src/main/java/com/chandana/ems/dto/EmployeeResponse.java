package com.yourname.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.yourname.ems.model.Department;
import com.yourname.ems.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee representation returned by the API")
public record EmployeeResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "John") String firstName,
        @Schema(example = "Doe") String lastName,
        @Schema(example = "john.doe@example.com") String email,
        @Schema(example = "9876543210") String phone,
        @Schema(example = "IT") Department department,
        @Schema(example = "Software Engineer") String designation,
        @Schema(example = "75000.00") BigDecimal salary,
        @Schema(example = "2024-01-15", type = "string", format = "date") LocalDate dateOfJoining,
        @Schema(example = "ACTIVE") Status status) {
}
