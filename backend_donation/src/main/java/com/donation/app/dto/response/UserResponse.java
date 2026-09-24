package com.donation.app.dto.response;


import java.time.LocalDateTime;

public record UserResponse (
    Long id,
    String name,
    String email,
    String imageUrl,
    Boolean active,
    LocalDateTime createdAccount,
    String rolName
){}