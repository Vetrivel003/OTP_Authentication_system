package com.project.spring.util;

import org.springframework.stereotype.Component;

@Component
public class MaskUtil {

    public String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + email.substring(atIndex);
        return email.substring(0, Math.min(3, atIndex)) + "***" + email.substring(atIndex);
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 3) + "*****" + phone.substring(phone.length() - 4);
    }

    public String maskIdentifier(String identifier) {
        if (identifier == null) return null;
        return identifier.contains("@") ? maskEmail(identifier) : maskPhone(identifier);
    }
}
