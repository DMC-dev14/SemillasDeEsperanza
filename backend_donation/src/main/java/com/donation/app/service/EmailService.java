package com.donation.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.donation.app.dto.request.EmailRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.urlFrontend}")
    private String urlFrontend;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public void sendEmailTemplate(EmailRequest emailRequest) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("userName", emailRequest.userName());
            context.setVariable("url", urlFrontend + emailRequest.jwt());

            helper.setFrom(mailFrom);
            helper.setTo(emailRequest.mailTo());
            helper.setSubject(emailRequest.mailSubject());
            helper.setText(templateEngine.process("email-template", context), true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo enviar el correo", e);
        }
    }
}