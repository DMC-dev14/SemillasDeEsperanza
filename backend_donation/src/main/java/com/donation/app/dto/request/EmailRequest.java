package com.donation.app.dto.request;

public record EmailRequest(
    String mailFrom,
    String mailTo,
    String mailSubject,
    String userName,
    String jwt
) {}
