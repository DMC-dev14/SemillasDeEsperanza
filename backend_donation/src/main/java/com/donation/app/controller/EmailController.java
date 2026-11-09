package com.donation.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.donation.app.dto.request.EmailRequest;
import com.donation.app.service.EmailService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class EmailController {
    
    private final EmailService emailService;

    @PostMapping("/email/send-html")
    public ResponseEntity<?> sendEmailTemplate(@RequestBody EmailRequest emailRequest){
        emailService.sendEmailTemplate(emailRequest);
        return new ResponseEntity("Correo enviado correctamente", HttpStatus.OK);
    }
}
