package edu.colorado.cs.epic.filteringapi.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.DatasetListOption;
import java.util.logging.Logger;

/**
 * Created by admin on 12/17/19.
 */
public class GoogleBigQueryHealthCheck extends HealthCheck {


    private final Logger logger;
    private final BigQuery bigquery;

    public GoogleBigQueryHealthCheck(BigQuery bigquery) {
        this.bigquery = bigquery;
        this.logger = Logger.getLogger(GoogleBigQueryHealthCheck.class.getName());

    }

    @Override
    protected HealthCheck.Result check() {
        try {
            bigquery.listDatasets(DatasetListOption.pageSize(100)).getValues();
            return HealthCheck.Result.healthy();
        } catch (Exception e) {
            return HealthCheck.Result.unhealthy(e);
        }
    }

}