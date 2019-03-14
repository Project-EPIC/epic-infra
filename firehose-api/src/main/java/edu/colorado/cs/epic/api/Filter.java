package edu.colorado.cs.epic.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1DeploymentBuilder;
import io.kubernetes.client.models.V1EnvVar;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Created by admin on 12/3/19.
 */
@JsonSnakeCase
public class Filter {

    private List<String> keywords;
    private String eventName;
    private URI url;

    public static String toDeploymentName(String eventName){
        return eventName + "-tweet-filter";
    }

    public Filter() {
        // Jackson deserialization
    }

    public Filter(List<String> keywords, String eventName) {
        this.keywords = keywords;
        this.eventName = eventName;

    }

    public Filter(V1Deployment deployment) {
        List<V1EnvVar> envs = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        for (V1EnvVar env : envs) {
            if (env.getName().equals("EVENT_NAME"))
                eventName = env.getValue();
            if (env.getName().equals("KEYWORDS"))
                keywords = Arrays.asList(env.getValue().split(","));
        }
        if (eventName == null || keywords == null){
            throw new IllegalStateException();
        }

    }

    @JsonProperty
    public String getEventName() {
        return eventName;
    }

    @JsonProperty
    public void setEventName(String eventName) {
        eventName = eventName
                .toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^\\x00-\\x7F]", "");
        this.eventName = eventName;
    }

    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty
    public URI getUrl() {
        return url;
    }

    @JsonProperty
    public void setUrl(URI url) {
        this.url = url;
    }

    public V1Deployment toDeployment(String kafkaServers, String tweetStoreVersion) {
        return new V1DeploymentBuilder()
                .editOrNewMetadata()
                .withName(deployName())
                .addToLabels("event", getEventName())
                .addToLabels("app", "tweet-filter")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .addToMatchLabels("event", getEventName())
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("event", getEventName())
                .addToLabels("app", "tweet-filter")
                .endMetadata()
                .withNewSpec()
                .withNewTerminationGracePeriodSeconds(10)
                .addNewContainer()
                .withName("tweet-filter")
                .withImage("projectepic/tweet-store:"+tweetStoreVersion)
                .withImagePullPolicy("Always")
                .withNewResources()
//                .addToLimits("cpu", Quantity.fromString("200m"))
                .addToLimits("memory", Quantity.fromString("800Mi"))
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
                .withValue(getEventName())
                .endEnv()
                .addNewEnv()
                .withName("KEYWORDS")
                .withValue(String.join(",", getKeywords()))
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

    public String deployName() {
        return toDeploymentName(getEventName());
    }
}
