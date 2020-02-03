package edu.colorado.cs.epic.geoupdateapi.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Storage;

import java.util.logging.Logger;

public class GoogleCloudStorageHealthCheck extends HealthCheck {

    private final Logger logger;
    private final Storage storage;

    public GoogleCloudStorageHealthCheck(Storage storage) {
        this.storage = storage;
        this.logger = Logger.getLogger(GoogleCloudStorageHealthCheck.class.getName());

    }

    @Override
    protected HealthCheck.Result check() {
        try {
            storage.list();
            return HealthCheck.Result.healthy();
        } catch (Exception e) {
            return HealthCheck.Result.unhealthy(e);
        }
    }

}