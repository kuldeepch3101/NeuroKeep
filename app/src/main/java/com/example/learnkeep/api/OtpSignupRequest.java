package com.example.learnkeep.api;

public class OtpSignupRequest {
    public String name;
    public String email;
    public String password;
    public String otp;

    public OtpSignupRequest(String name, String email, String password, String otp) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.otp      = otp;
    }
}
