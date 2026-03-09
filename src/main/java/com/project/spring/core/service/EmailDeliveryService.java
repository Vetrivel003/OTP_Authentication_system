package com.project.spring.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    public void send(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp + "\nThis OTP is valid for 5 minutes. Do not share it with anyone.");
        mailSender.send(message);
    }
}
