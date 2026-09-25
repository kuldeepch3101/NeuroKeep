package com.example.learnkeep.api;

public class SignupRequest {

    public String name;
    public String email;
    public String password;

    public SignupRequest(String name,String email,String password){
        this.name=name;
        this.email=email;
        this.password=password;
    }
}