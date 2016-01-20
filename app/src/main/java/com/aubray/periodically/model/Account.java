package com.aubray.periodically.model;

/**
 * A user
 */
public class Account {
    public String userName, email, photoUrl;

    public Account(String email, String userName, String photoUrl) {
        this.email = email;
        this.userName = userName;
        this.photoUrl = photoUrl;
    }
}
