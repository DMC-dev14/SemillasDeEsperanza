package com.donation.app.dto.response;

import java.time.Instant;

public record AuthResponse(
    String token,
    UserResponse user,
    Instant expiration
) {}
