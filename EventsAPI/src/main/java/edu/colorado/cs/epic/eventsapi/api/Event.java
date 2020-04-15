package edu.colorado.cs.epic.eventsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.*;
import org.hibernate.validator.constraints.NotEmpty;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotNull;

@JsonSnakeCase
public class Event {

    public enum Status {
        ACTIVE, NOT_ACTIVE, FAILED
    }

    private enum MatchKey {
        KEYWORDS("tweets", "epic-collect"), FOLLOWS("tweets-follow", "epic-collect-follow");
        private final String kafkaTopic;
        private final String bucketName;
        private MatchKey(String kafkaTopic, String bucketName) {
            this.kafkaTopic = kafkaTopic;
            this.bucketName = bucketName;
        }
        public String getKafkaTopic() {
            return this.kafkaTopic;
        }
        public String getBucketName() {
            return this.bucketName;
        }
    }

    @NotEmpty
    private String name;
    @NotNull
    private MatchKey matchKey; // Default to KEYWORDS to be backwards compatible
    @NotEmpty
    private List<String> keywords = new ArrayList<String>();
    @NotEmpty
    private String description;
    private String normalizedName = null;
    private Status status;

    private Timestamp createdAt;


    private String author;

    public static String toDeploymentName(String name) {
        return name + "-tweet-filter";
    }

    public static String toBigqueryTableName(String name) {
        return name.replace("-", "_");
    }

    public static String toAutoscalerName(String name) {
        return toDeploymentName(name)+"-scaler";
    }


    public Event() {
        // Jackson deserialization

    }

    public Event(V1Deployment deployment) {
        List<V1EnvVar> envs = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        for (V1EnvVar env : envs) {
            if (env.getName().equals("EVENT_NAME"))
                normalizedName = env.getValue();
            if (env.getName().equals("KEYWORDS"))
                keywords = Arrays.asList(env.getValue().split(","));
            if (env.getName().equals("MATCH_KEY"))
                setMatchKey(env.getValue());
        }
        if (normalizedName == null || keywords == null) {
            throw new IllegalStateException();
        }

    }


    public Event(String name, List<String> keywords, String description) {
        this.name = name;
        this.keywords = keywords;
        this.description = description;
        this.normalizedName = normalizeName(name);
        this.status = Status.ACTIVE;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
    
    
    public Event(String name, List<String> keywords, String match_key, String description) {
        this.name = name;
        this.keywords = keywords;
        this.description = description;
        this.normalizedName = normalizeName(name);
        this.status = Status.ACTIVE;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.setMatchKey(match_key);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }


    @JsonProperty
    public String getNormalizedName() {
        return normalizedName;
    }

    @JsonProperty
    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
        this.normalizedName = normalizeName(name);
    }

    @JsonProperty("match_key")
    public String getMatchKey() {
        return matchKey.name();
    }

    @JsonProperty("match_key")
    public void setMatchKey(String match_key) {
        switch (match_key.toUpperCase()) {
            case "FOLLOWS":
                this.matchKey = MatchKey.FOLLOWS;
                break;
            case "KEYWORDS":
            default:
                this.matchKey = MatchKey.KEYWORDS;
                break;
        }
    }

    public void setMatchKey(MatchKey matchKey) {
        this.matchKey = matchKey;
    }


    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void appendKeywords(String name) {
        this.keywords.add(name);
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    @JsonProperty
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty
    public String getStatus() {
        return status.name();
    }

    @JsonProperty
    public void setStatus(String status) {
        this.status = Event.Status.valueOf(status);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonProperty
    public void setCreatedAtStr(String createdAt) {
        this.createdAt = Timestamp.valueOf(createdAt);
    }

    @JsonProperty
    public String getCreatedAtStr() {
        return this.createdAt.toString();
    }

    @JsonProperty
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @JsonProperty
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }


    public static String normalizeName(String name) {
        return name.replaceAll("\\s+", "-")
                .replaceAll("[^-a-zA-Z0-9]", "").toLowerCase();
    }

    public String deployName() {
        return toDeploymentName(normalizedName);
    }

    public String autoscalerName() {return toAutoscalerName(normalizedName);}

    public String bigQueryTableName() {
        return toBigqueryTableName(normalizedName);
    }


    public V1Deployment toDeployment(String kafkaServers, String tweetStoreVersion) {
        return new V1DeploymentBuilder()
                .editOrNewMetadata()
                .withName(deployName())
                .addToLabels("event", normalizedName)
                .addToLabels("app", "tweet-filter")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .addToMatchLabels("event", normalizedName)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("event", normalizedName)
                .addToLabels("app", "tweet-filter")
                .endMetadata()
                .withNewSpec()
                .withNewTerminationGracePeriodSeconds(120)
                .addNewContainer()
                .withName("tweet-filter")
                .withImage("projectepic/tweet-store:" + tweetStoreVersion)
                .withImagePullPolicy("Always")
                .withNewResources()
//                .addToLimits("cpu", Quantity.fromString("200m"))
                .addToLimits("memory", Quantity.fromString("500Mi"))
                .addToRequests("memory", Quantity.fromString("100Mi"))
                .endResources()
                .addNewEnv()
                .withName("KAFKA_SERVER")
                .withValue(kafkaServers)
                .endEnv()
                .addNewEnv()
                .withName("BATCH_SIZE")
                .withValue("1000")
                .endEnv()
                .addNewEnv()
                .withName("EVENT_NAME")
                .withValue(normalizedName)
                .endEnv()
                .addNewEnv()
                .withName("KAFKA_TOPIC")
                .withValue(this.matchKey.getKafkaTopic())
                .endEnv()
                .addNewEnv()
                .withName("BUCKET_NAME")
                .withValue(this.matchKey.getBucketName())
                .endEnv()
                .addNewEnv()
                .withName("MATCH_KEY") // MATCH_KEY determines what tweet bucketing algorithm will be used in tweet-store
                .withValue(this.matchKey.name())
                .endEnv()
                .addNewEnv()
                .withName(this.matchKey.name()) // Environment variable that depends on MATCH_KEY value
                .withValue(String.join(",", keywords))
                .endEnv()
                .addNewEnv()
                .withName("GOOGLE_APPLICATION_CREDENTIALS")
                .withValue("/private/keyfile.json")
                .endEnv()
                .addNewVolumeMount()
                .withName("keyfiles")
                .withMountPath("/private")
                .endVolumeMount()
                .endContainer()
                .addNewVolume()
                .withName("keyfiles")
                .editOrNewSecret()
                .withSecretName("keyfile")
                .endSecret()
                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

    }

    public V1HorizontalPodAutoscaler toAutoScaler() {
        return new V1HorizontalPodAutoscalerBuilder()
                .editOrNewMetadata()
                .withName(autoscalerName())
                .endMetadata()
                .withNewSpec()
                .editOrNewScaleTargetRef()
                .withApiVersion("apps/v1beta1")
                .withKind("Deployment")
                .withName(deployName())
                .endScaleTargetRef()
                .withMinReplicas(1)
                .withMaxReplicas(3)
                .withNewTargetCPUUtilizationPercentage(90)
                .endSpec()
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Event)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        Event c = (Event) o;

        // Compare the data members and return accordingly
        return normalizedName.equals(c.getNormalizedName());
    }

    @Override
    public int hashCode() {
        return normalizedName.hashCode();
    }
}