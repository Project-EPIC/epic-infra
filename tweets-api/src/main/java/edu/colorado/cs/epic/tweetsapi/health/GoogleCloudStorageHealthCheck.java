package edu.colorado.cs.epic.tweetsapi.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.util.logging.Logger;

/**
 * Created by admin on 10/4/19.
 */
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
            storage.list("epic-collect", Storage.BlobListOption.pageSize(1)).getValues();
            return HealthCheck.Result.healthy();
        } catch (Exception e) {
            return HealthCheck.Result.unhealthy(e);
        }
    }

}