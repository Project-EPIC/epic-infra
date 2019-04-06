package edu.colorado.cs.epic.eventsapi.api;

/**
 * Created by admin on 5/4/19.
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.UserRecord;

import java.net.URI;
import java.security.Principal;

public class User implements Principal {
    private String uid;
    private Boolean admin;
    private Boolean disabled;


    public User(String email, Boolean admin, URI photoURL, String uid) {

        this.admin = admin;

        this.uid = uid;
    }

    public User(ExportedUserRecord firebaseUser) {

        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);

        this.uid = firebaseUser.getUid();
        this.disabled = firebaseUser.isDisabled();
    }


    public User(UserRecord firebaseUser) {
        this.admin = (Boolean) firebaseUser.getCustomClaims().getOrDefault("admin", false);
        this.uid = firebaseUser.getUid();
        this.disabled = firebaseUser.isDisabled();
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

