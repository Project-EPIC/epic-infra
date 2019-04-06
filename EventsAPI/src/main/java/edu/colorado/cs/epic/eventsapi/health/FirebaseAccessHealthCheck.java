package edu.colorado.cs.epic.eventsapi.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.logging.Logger;

/**
 * Created by admin on 5/4/19.
 */
public class FirebaseAccessHealthCheck extends HealthCheck {

    private final Logger logger;

    public FirebaseAccessHealthCheck() {

        this.logger = Logger.getLogger(FirebaseAccessHealthCheck.class.getName());

    }

    @Override
    protected HealthCheck.Result check() {

        try {
            FirebaseAuth.getInstance().listUsers(null);
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            logger.warning("Firebase Auth connection is failing");
            return HealthCheck.Result.unhealthy("Firebase Auth connection is failing");
        }
        return HealthCheck.Result.healthy();
    }

}
