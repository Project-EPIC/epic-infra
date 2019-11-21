package edu.colorado.cs.epic.eventsapi.health;

import com.codahale.metrics.health.HealthCheck;
import edu.colorado.cs.epic.eventsapi.core.KubernetesController;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1PodList;

import java.util.logging.Logger;

/**
 * Created by admin on 7/3/19.
 */
public class KubernetesConnectionHealthCheck extends HealthCheck {
    private final KubernetesController client;
    private final Logger logger;

    public KubernetesConnectionHealthCheck(KubernetesController k8scontroller) {
        this.client = k8scontroller;
        this.logger = Logger.getLogger(KubernetesConnectionHealthCheck.class.getName());

    }

    @Override
    protected Result check() {
        V1PodList pods = null;
        try {
            pods = client.getAllPods();
        } catch (ApiException e) {
            e.printStackTrace();
            logger.warning("Kubernetes cluster connection is failing");
            return Result.unhealthy("Kubernetes cluster connection is failing");
        }

        if (pods.getItems().isEmpty()) {
            logger.warning("Not connected to Kubernetes!");
            return Result.unhealthy("Not connected to Kubernetes cluster");
        } else {
            return Result.healthy();
        }
    }

}
