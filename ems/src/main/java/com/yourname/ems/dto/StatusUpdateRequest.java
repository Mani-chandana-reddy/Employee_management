package com.yourname.ems.dto;

import com.yourname.ems.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload used to activate or deactivate an employee")
public record StatusUpdateRequest(
        @Schema(example = "INACTIVE", description = "ACTIVE or INACTIVE")
        @NotNull(message = "Status is required")
        Status status) {
}
