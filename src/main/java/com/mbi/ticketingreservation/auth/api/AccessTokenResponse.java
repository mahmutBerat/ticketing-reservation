package com.mbi.ticketingreservation.auth.api;

public record AccessTokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn
) {
}
