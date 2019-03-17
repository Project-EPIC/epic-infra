package eventapi;
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


    @NotEmpty
    private String kafkaServers;

    @NotEmpty
    private String namespace;

    @NotEmpty
    private String tweetStoreVersion;

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

}