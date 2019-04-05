package edu.colorado.cs.epic.auth.auth;

import edu.colorado.cs.epic.auth.api.User;
import io.dropwizard.auth.Authorizer;

import java.util.Objects;


public class FirebaseAuthorizator implements Authorizer<User> {
    @Override
    public boolean authorize(User user, String role) {
        if (Objects.equals(role, "ADMIN")) {
            return user.getAdmin();
        }
        return false;

    }
}