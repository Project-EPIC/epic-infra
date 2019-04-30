package edu.colorado.cs.epic.eventsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1DeploymentBuilder;
import io.kubernetes.client.models.V1EnvVar;
import org.hibernate.validator.constraints.NotEmpty;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonSnakeCase
public class Event {


    public enum Status {
        ACTIVE, NOT_ACTIVE, FAILED
    }

    @NotEmpty
    private String name;
    @NotEmpty
    private List<String> keywords = new ArrayList<String>();
    @NotEmpty
    private String description;
    private String normalizedName = null;
    private Status status;

    private Timestamp createdAt;

    private URI bigQueryTableURL;

    private String author;

    public static String toDeploymentName(String name) {
        return name + "-tweet-filter";
    }

    public static String toBigqueryTableName(String name) {
        return name.replace("-", "_");
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonProperty
    public URI getBigQueryTableURL() {
        if (bigQueryTableURL == null)
            bigQueryTableURL = URI.create(String.format("https://console.cloud.google.com/bigquery?project=crypto-eon-164220&p=crypto-eon-164220&d=tweets&t=%s&page=table", this.bigQueryTableName()));
        return bigQueryTableURL;
    }

    @JsonProperty
    public void setBigQueryTableURL(URI bigQueryTableURL) {
        this.bigQueryTableURL = bigQueryTableURL;
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
                .withNewTerminationGracePeriodSeconds(10)
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
                .withName("KEYWORDS")
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