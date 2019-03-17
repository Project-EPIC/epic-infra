package eventapi.resource;

import eventapi.api.Filter;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.models.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Created by admin on 12/3/19.
 */
public class FilterResource {

    private final Logger logger;
    private final ApiClient client;
    private final String kafkaServers;
    private final String namespace;
    private final String tweetStoreVersion;

    public FilterResource(ApiClient client, String kafkaServers, String tweetStoreVersion, String namespace) {
        this.client = client;
        this.kafkaServers = kafkaServers;
        this.tweetStoreVersion = tweetStoreVersion;
        this.namespace = namespace;
        this.logger = Logger.getLogger(QueryResource.class.getName());
    }

    public List<Filter> fetch() {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        V1DeploymentList deploys = new V1DeploymentList();
        try {
            deploys = api.listNamespacedDeployment(namespace, false, null, null, null, "app=tweet-filter", null, null, null, false);
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        }
        List<Filter> filters = new ArrayList<>();
        for (V1Deployment deployment : deploys.getItems()) {
            try {
                Filter filter = new Filter(deployment);
                filters.add(filter);
            } catch (IllegalStateException e) {
                logger.info("Ignoring this deployment");
            }
        }
        return filters;
    }


    public Filter fetchOne( String eventName) {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        V1Deployment deploy = null;
        try {
            deploy = api.readNamespacedDeployment(Filter.toDeploymentName(eventName),namespace, null, false, false);
        } catch (ApiException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        try {
            Filter filter = new Filter(deploy);
            return filter;
        } catch (IllegalStateException e) {
            logger.warning("Kubernetes API gave the following error");
            return null;
        }
    }

    public boolean deleteOne(String eventName) {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        try {

            api.deleteNamespacedDeployment(Filter.toDeploymentName(eventName), namespace, new V1DeleteOptions(),null,null,null,null,null);
            return true;
        } catch (ApiException e) {
            logger.warning("Kubernetes API gave the following error");
            e.printStackTrace();
            return false;
        }

    }


    public boolean create(Filter filter) {
        V1Deployment deploy = filter.toDeployment(kafkaServers, tweetStoreVersion);
        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        try {
            api.createNamespacedDeployment(namespace, deploy, false, null, null);
            return true;
        } catch (ApiException e) {
            logger.info("Already existing deployment");
        }
        try {
            api.replaceNamespacedDeployment(filter.deployName(), namespace, deploy, null, null);
            return true;
        } catch (ApiException e) {
            logger.info("Already existing deployment");
            e.printStackTrace();
            return false;
        }
    }


}
