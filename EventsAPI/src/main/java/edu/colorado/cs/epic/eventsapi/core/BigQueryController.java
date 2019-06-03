package edu.colorado.cs.epic.eventsapi.core;

import com.google.cloud.bigquery.*;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.resource.EventResource;
import org.apache.log4j.Logger;

import java.net.URI;

import static com.google.api.client.util.Charsets.UTF_8;

public class BigQueryController {

    private final String bucketName;
    private final Logger logger;

    public BigQueryController(String bucketName) {
        this.bucketName = bucketName;
        this.logger = Logger.getLogger(EventResource.class.getName());

    }

    public void createBigQueryTable(Event event) {
        if (tableExists(event)){
            return;
        }

        // Create big query table
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
        TableId tableId = TableId.of("tweets", event.bigQueryTableName());
        ExternalTableDefinition x = ExternalTableDefinition
                .newBuilder(
                        "gs://epic-collect/" + event.getNormalizedName() + "/*",
                        null,
                        FormatOptions.json()
                ).setMaxBadRecords(Integer.MAX_VALUE)
                .setIgnoreUnknownValues(true)
                .setCompression("GZIP")
                .setAutodetect(true)
                .build();
        TableInfo tableInfo = TableInfo.newBuilder(tableId, x).build();
        bigquery.create(tableInfo, BigQuery.TableOption.fields(BigQuery.TableField.EXTERNAL_DATA_CONFIGURATION));
        logger.info(String.format("Created big query table %s", event.bigQueryTableName()));
    }

    public Boolean tableExists(Event event) {
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
        Table table;
        try {
            table = bigquery.getTable(TableId.of("tweets", event.bigQueryTableName()));
        } catch (Exception e) {
            return false;
        }
        return table != null;
    }

}

