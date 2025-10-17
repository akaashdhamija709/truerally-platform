package com.truerally.auth.util;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    private static final String BASE_URL = "http://localhost:8081/auth";

    public String buildVerificationEmail(String name, String token) {
        String link = BASE_URL + "/verify?token=" + token;
        return "<h3>Welcome " + name + "!</h3>"
                + "<p>Please verify your email by clicking the link below:</p>"
                + "<a href='" + link + "'>" + link + "</a>";
    }

    public String buildPasswordResetEmail(String name, String token) {
        String link = BASE_URL + "/reset-password/validate?token=" + token;
        return "<p>Hi " + name + ",</p>"
                + "<p>Click below to reset your password:</p>"
                + "<a href='" + link + "'>" + link + "</a>";
    }
}

