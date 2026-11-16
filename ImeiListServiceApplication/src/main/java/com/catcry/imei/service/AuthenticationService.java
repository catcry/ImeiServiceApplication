package com.catcry.imei.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Value("${imei.auth.username}")
    private String expectedUsername;

    @Value("${imei.auth.password}")
    private String expectedPassword;

    public boolean isValid(String username, String password) {
        return expectedUsername.equals(username) && expectedPassword.equals(password);
    }
}
