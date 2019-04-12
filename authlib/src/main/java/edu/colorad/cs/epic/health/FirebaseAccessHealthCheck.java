package edu.colorad.cs.epic.health;

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
    protected Result check() {

        try {
            FirebaseAuth.getInstance().listUsers(null);
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            logger.warning("Firebase Auth connection is failing");
            return Result.unhealthy("Firebase Auth connection is failing");
        }
        return Result.healthy();
    }

}
