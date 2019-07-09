package edu.colorado.cs.epic.eventsapi.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.*;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.resource.EventResource;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.api.client.util.Charsets.UTF_8;

public class BigQueryController {

    private final String bucketName;
    private final Logger logger;

    public BigQueryController(String bucketName) {
        this.bucketName = bucketName;
        this.logger = Logger.getLogger(EventResource.class.getName());

    }

    public void createBigQueryTable(Event event) {
        if (tableExists(event)) {
            return;
        }

        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
        TableId tableId = TableId.of("tweets", event.bigQueryTableName());
        ExternalTableDefinition definition;
        try {


            String jsonSchema = new String(Files.readAllBytes(Paths.get("schema.json")));
            // Create big query table

            LinkedHashMap fieldList = new ObjectMapper().readValue(jsonSchema, LinkedHashMap.class);

            definition = ExternalTableDefinition
                    .newBuilder(
                            "gs://epic-collect/" + event.getNormalizedName() + "/*",
                            null,
                            FormatOptions.json()
                    ).setMaxBadRecords(Integer.MAX_VALUE)
                    .setIgnoreUnknownValues(true)
                    .setCompression("GZIP")
                    .setSchema(createSchema(fieldList))
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            definition = ExternalTableDefinition
                    .newBuilder(
                            "gs://epic-collect/" + event.getNormalizedName() + "/*",
                            null,
                            FormatOptions.json()
                    ).setMaxBadRecords(Integer.MAX_VALUE)
                    .setIgnoreUnknownValues(true)
                    .setCompression("GZIP")
                    .setAutodetect(true)
                    .build();
        }

        TableInfo tableInfo = TableInfo.newBuilder(tableId, definition).build();
        bigquery.create(tableInfo, BigQuery.TableOption.fields(BigQuery.TableField.EXTERNAL_DATA_CONFIGURATION));
        logger.info(String.format("Created big query table %s", event.bigQueryTableName()));
    }

    static private Schema createSchema(LinkedHashMap tableSchema) {
        List<LinkedHashMap> tableSchemaFields = (List<LinkedHashMap>) tableSchema.get("fields");
        List<Field> fields = tableSchemaFields.stream().map(BigQueryController::fromPb).collect(Collectors.toList());
        return Schema.of(fields);
    }

    static private Field fromPb(LinkedHashMap fieldSchemaPb) {
        List<LinkedHashMap> fields = (List<LinkedHashMap>) fieldSchemaPb.get("fields");
        FieldList subFields = fields != null ?
                FieldList.of(fields.stream().map(BigQueryController::fromPb).collect(Collectors.toList()))
                : null;

        LegacySQLTypeName type = LegacySQLTypeName.valueOf((String) fieldSchemaPb.get("type"));
        String name = (String) fieldSchemaPb.get("name");
        return Field.newBuilder(name,type,subFields).build();
    }


    public Boolean tableExists(Event event) {
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

        try {
//            TableId tableId = TableId.of();
            for (Table table : bigquery.listTables("tweets").iterateAll()) {
                if (table.getGeneratedId().split("\\.")[1].equals(event.bigQueryTableName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

}

