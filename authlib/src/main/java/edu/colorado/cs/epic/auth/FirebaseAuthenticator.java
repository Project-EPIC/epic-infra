package edu.colorado.cs.epic.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import edu.colorado.cs.epic.api.FirebaseUser;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by admin on 5/4/19.
 */
public class FirebaseAuthenticator implements Authenticator<String, FirebaseUser> {


    private final Logger logger;

    public FirebaseAuthenticator() {
        this.logger = Logger.getLogger(FirebaseAuthenticator.class.getName());
    }

    @Override
    public Optional<FirebaseUser> authenticate(String credentials) throws AuthenticationException {


        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(credentials);
            UserRecord firebaseUser = FirebaseAuth.getInstance().getUser(decodedToken.getUid());
            if (firebaseUser.isDisabled()) {
                logger.info(String.format("Trying to log in from a disabled user: %s", firebaseUser.getEmail()));
                return Optional.empty();
            }
            FirebaseUser authenticatedFirebaseUser = new FirebaseUser(firebaseUser);
            return Optional.of(authenticatedFirebaseUser);
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
