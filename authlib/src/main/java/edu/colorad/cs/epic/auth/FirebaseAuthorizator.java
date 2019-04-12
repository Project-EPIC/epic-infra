package edu.colorad.cs.epic.auth;

/**
 * Created by admin on 5/4/19.
 */

import edu.colorad.cs.epic.api.FirebaseUser;
import io.dropwizard.auth.Authorizer;

import java.util.Objects;


public class FirebaseAuthorizator implements Authorizer<FirebaseUser> {
    @Override
    public boolean authorize(FirebaseUser FirebaseUser, String role) {
        if (Objects.equals(role, "ADMIN")) {
            return FirebaseUser.getAdmin();
        }
        return false;

    }
}