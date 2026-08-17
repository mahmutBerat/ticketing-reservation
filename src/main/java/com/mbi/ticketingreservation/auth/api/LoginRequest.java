package com.mbi.ticketingreservation.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        @Schema(example = "customer@example.com") String email,

        @NotBlank
        @Size(max = 72, min = 8)
        @Schema(example = "Customer123!") String password
) {
}
