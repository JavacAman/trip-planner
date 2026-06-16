package com.trao.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// SOLID-SRP: Handles only day regeneration request input
@Data
public class ModifyDayRequest {

    @NotBlank(message = "Modification instruction is required")
    private String instruction;
}
