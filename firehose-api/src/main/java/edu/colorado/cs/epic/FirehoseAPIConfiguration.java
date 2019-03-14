package edu.colorado.cs.epic;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.*;

public class FirehoseAPIConfiguration extends Configuration {

    @NotEmpty
    private String firehoseConfigMapName;


    @NotEmpty
    private String kafkaServers;

    @NotEmpty
    private String firehoseConfigMapNamespace;

    @NotEmpty
    private String tweetStoreVersion;

    @JsonProperty
    public String getFirehoseConfigMapNamespace() {
        return firehoseConfigMapNamespace;
    }

    @JsonProperty
    public void setFirehoseConfigMapNamespace(String firehoseConfigMapNamespace) {
        this.firehoseConfigMapNamespace = firehoseConfigMapNamespace;
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
}
