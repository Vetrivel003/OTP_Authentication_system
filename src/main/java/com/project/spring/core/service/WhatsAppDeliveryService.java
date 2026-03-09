package com.project.spring.core.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppDeliveryService {

    @Value("${twilio.account-sid}") private String accountSid;
    @Value("${twilio.auth-token}") private String authToken;
    @Value("${twilio.whatsapp-from}") private String whatsappFrom;

    @PostConstruct
    public void init() { Twilio.init(accountSid, authToken); }

    public void send(String toPhone, String otp) {
        Message.creator(
                new PhoneNumber("whatsapp:" + toPhone),
                new PhoneNumber(whatsappFrom),
                "Your OTP is: *" + otp + "*\nThis is valid for 5 minutes."
        ).create();
    }
}
