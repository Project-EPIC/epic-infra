package edu.colorado.cs.epic.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.UserRecord;

import javax.security.auth.Subject;
import java.net.URI;
import java.security.Principal;


public class User implements Principal {
    private String uid;
    private String email;
    private Boolean admin;
    private URI photoURL;


    public User(String email, Boolean admin, URI photoURL, String uid) {
        this.email = email;
        this.admin = admin;
        this.photoURL = photoURL;
        this.uid = uid;
    }

    public User(ExportedUserRecord firebaseUser) {
        this.email = firebaseUser.getEmail();
        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);
        this.photoURL = URI.create(firebaseUser.getPhotoUrl());
        this.uid = firebaseUser.getUid();
    }


    public User(UserRecord firebaseUser) {
        this.email = firebaseUser.getEmail();
        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);
        this.photoURL = URI.create(firebaseUser.getPhotoUrl());
        this.uid = firebaseUser.getUid();
    }

    public User() {

    }

    @JsonProperty
    public Boolean getAdmin() {
        return admin;
    }

    @JsonProperty
    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    @JsonProperty
    public String getEmail() {
        return email;
    }

    @JsonProperty
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty
    public URI getPhotoURL() {
        return photoURL;
    }

    @JsonProperty
    public void setPhotoURL(URI photoURL) {
        this.photoURL = photoURL;
    }

    @JsonProperty
    public String getUid() {
        return uid;
    }

    @JsonProperty
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String getName() {
        return uid;
    }
}
