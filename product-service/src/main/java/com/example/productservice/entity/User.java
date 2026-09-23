package com.example.productservice.entity;

import java.io.Serializable;


public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;

    public User() {
    }

    public User(String id, String fullName, String email, String phoneNumber) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', fullName='" + fullName +
                "', email='" + email + "', phoneNumber='" + phoneNumber + "'}";
    }
}
