package com.aubray.periodically.model;

public class User {

    private String uid;
    private String email;
    private String givenName;
    private String familyName;
    private String profileImageURL;

    public User() {
        // Do not use: For Firebase
    }

    public User(String uid, String email, String givenName, String familyName) {
        this.uid = uid;
        this.email = email;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getProfileImageURL() {
        return profileImageURL;
    }

    public void setProfileImageURL(String profileImageURL) {
        this.profileImageURL = profileImageURL;
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                '}';
    }

    public String fullName() {
        return givenName + " " + familyName;
    }
}
