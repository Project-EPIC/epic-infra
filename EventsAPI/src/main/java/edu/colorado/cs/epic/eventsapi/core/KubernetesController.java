package edu.colorado.cs.epic.eventsapi.core;

import com.codahale.metrics.health.HealthCheck;
import edu.colorado.cs.epic.eventsapi.api.Event;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.*;


/**
 * Created by admin on 19/3/19.
 */
public class KubernetesController {

    private final ApiClient client;
    private final String kafkaServers;
    private final String tweetStoreVersion;
    private final String namespace;
    private String configMapName;
    private final Logger logger;

    public KubernetesController(ApiClient client, String kafkaServers, String tweetStoreVersion, String namespace, String configMapName) {
        this.client = client;
        this.kafkaServers = kafkaServers;
        this.tweetStoreVersion = tweetStoreVersion;
        this.namespace = namespace;
        this.configMapName = configMapName;
        this.logger = Logger.getLogger(KubernetesController.class.getName());
    }

    public List<Event> getActiveEvents() {
        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        V1DeploymentList deploys = new V1DeploymentList();

        try {
            deploys = api.listNamespacedDeployment(namespace, false, null, null, null, "app=tweet-filter", null, null, null, false);
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        }
        List<Event> filters = new ArrayList<>();
        for (V1Deployment deployment : deploys.getItems()) {
            try {
                Event event = new Event(deployment);
                filters.add(event);
            } catch (IllegalStateException e) {
                logger.debug("Ignoring this deployment");
            }
        }
        return filters;
    }


    public Event getActiveEvent(String normalizedName) {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        V1Deployment deploy = null;
        try {
            deploy = api.readNamespacedDeployment(Event.toDeploymentName(normalizedName), namespace, null, false, false);
        } catch (ApiException e) {
            logger.error("Kubernetes API gave the following error", e);
            return null;
        }
        return new Event(deploy);
    }

    public void stopEvent(String normalizedName) throws ApiException {
        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();

        api.deleteNamespacedDeployment(Event.toDeploymentName(normalizedName), namespace, new V1DeleteOptions(), null, null, null, null, null);
    }

    public void startEvent(Event event) throws ApiException {
        V1Deployment deploy = event.toDeployment(kafkaServers, tweetStoreVersion);

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        try {
            api.createNamespacedDeployment(namespace, deploy, false, null, null);
        } catch (ApiException e) {
            logger.info("Already existing deployment");
            api.replaceNamespacedDeployment(event.deployName(), namespace, deploy, null, null);
        }

    }


    public List<String> getActiveStreamKeywords() throws ApiException {
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();


        V1ConfigMap config = api.readNamespacedConfigMap(configMapName, namespace, null, true, false);
        String keywords = config.getData().getOrDefault("keywords", "");
        return Arrays.asList(keywords.split(","));

    }

    public void setActiveStreamKeywords(List<String> keywords) throws ApiException {
        V1ConfigMap config = new V1ConfigMapBuilder()
                .addToData("keywords", String.join(",", keywords))
                .editOrNewMetadata()
                .withName(configMapName)
                .withNamespace(namespace)
                .endMetadata()
                .build();

        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        try {
            api.createNamespacedConfigMap(namespace, config, false, null, null);

        } catch (ApiException e) {
            logger.info("ConfigMap already existis. Replacing...");
            api.replaceNamespacedConfigMap(configMapName, namespace, config, null, null);
        }

    }

    public V1PodList getAllPods() throws ApiException {
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        return api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);


    }
}
