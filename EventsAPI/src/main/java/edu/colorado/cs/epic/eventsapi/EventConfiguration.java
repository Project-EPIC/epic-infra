package edu.colorado.cs.epic.eventsapi;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class EventConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @NotEmpty
    private String firehoseConfigMapName;

    private Boolean production;


    @NotEmpty
    private String kafkaServers;

    @NotEmpty
    private String namespace;

    @NotEmpty
    private String tweetStoreVersion;

    @NotEmpty
    private String gcloudProjectID;

    @NotEmpty
    private  String templateNameDataproc;

    @JsonProperty
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonProperty
    public String getFirehoseConfigMapName() {
        return firehoseConfigMapName;
    }

    @JsonProperty
    public void setFirehoseConfigMapName(String firehoseConfigMapName) {
        this.firehoseConfigMapName = firehoseConfigMapName;
    }

    @JsonProperty
    public String getKafkaServers() {
        return kafkaServers;
    }

    @JsonProperty
    public void setKafkaServers(String kafkaServers) {
        this.kafkaServers = kafkaServers;
    }

    @JsonProperty
    public String getTweetStoreVersion() {
        return tweetStoreVersion;
    }

    @JsonProperty
    public void setTweetStoreVersion(String tweetStoreVersion) {
        this.tweetStoreVersion = tweetStoreVersion;
    }
    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty
    public Boolean getProduction() {
        return production;
    }

    @JsonProperty
    public void setProduction(Boolean production) {
        this.production = production;
    }

    @JsonProperty
    public String getGcloudProjectID() {
        return gcloudProjectID;
    }

    @JsonProperty
    public void setGcloudProjectID(String gcloudProjectID) {
        this.gcloudProjectID = gcloudProjectID;
    }

    @JsonProperty
    public String getTemplateNameDataproc() {
        return templateNameDataproc;
    }

    @JsonProperty
    public void setTemplateNameDataproc(String templateNameDataproc) {
        this.templateNameDataproc = templateNameDataproc;
    }
}