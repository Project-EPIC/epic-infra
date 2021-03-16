package edu.colorado.cs.epic.geotagapi.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

import org.apache.log4j.Logger;

public class GoogleCloudStorageHealthCheck extends HealthCheck {
    private final Logger logger;
    private final Bucket storage;

    public GoogleCloudStorageHealthCheck(Bucket storage) {
        this.storage = storage;
        this.logger = Logger.getLogger(GoogleCloudStorageHealthCheck.class.getName());

    }

    @Override
    protected HealthCheck.Result check() {
        try {
            storage.list(Storage.BlobListOption.pageSize(1)).getValues();
            return HealthCheck.Result.healthy();
        } catch (Exception e) {
            return HealthCheck.Result.unhealthy(e);
        }
    }
}
