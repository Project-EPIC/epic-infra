package edu.colorado.cs.epic.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.UserRecord;

import java.net.URI;
import java.security.Principal;

public class FirebaseUser implements Principal {
    private String uid;
    private Boolean admin;
    private Boolean disabled;
    private String email;
    private URI photoURL;


    public FirebaseUser(String email, Boolean admin, URI photoURL, String uid) {

        this.admin = admin;

        this.uid = uid;
    }

    public FirebaseUser(ExportedUserRecord firebaseUser) {
        this.email = firebaseUser.getEmail();
        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);
        this.photoURL = URI.create(firebaseUser.getPhotoUrl());
        this.uid = firebaseUser.getUid();
        this.disabled = firebaseUser.isDisabled();
    }


    public FirebaseUser(UserRecord firebaseUser) {
        this.email = firebaseUser.getEmail();
        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);
        this.photoURL = URI.create(firebaseUser.getPhotoUrl());
        this.uid = firebaseUser.getUid();
        this.disabled = firebaseUser.isDisabled();
    }

    public FirebaseUser() {

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

    @JsonProperty
    public Boolean getDisabled() {
        return disabled;
    }

    @JsonProperty
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}

