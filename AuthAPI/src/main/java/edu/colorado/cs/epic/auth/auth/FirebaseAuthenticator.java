package edu.colorado.cs.epic.auth.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import edu.colorado.cs.epic.auth.api.User;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;

/**
 * Created by admin on 4/4/19.
 */
public class FirebaseAuthenticator implements Authenticator<String, User> {
    @Override
    public Optional<User> authenticate(String credentials) throws AuthenticationException {


        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(credentials);
            User authenticatedUser = new User(FirebaseAuth.getInstance().getUser(decodedToken.getUid()));
            return Optional.of(authenticatedUser);
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}