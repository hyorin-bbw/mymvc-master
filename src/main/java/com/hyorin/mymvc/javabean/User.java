package com.hyorin.mymvc.javabean;

public class User {

    public String name;
    public String email;

    public String password;
    public String description;

    public User(String name, String email, String password, String description) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.description = description;
    }

    public User() {
    }
}
