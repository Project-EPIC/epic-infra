package edu.colorado.cs.epic.resources;

import edu.colorado.cs.epic.api.Filter;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.models.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by admin on 12/3/19.
 */
@Path("/filters/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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

    @GET
    public List<Filter> fetch(@Context UriInfo uriInfo) {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        V1DeploymentList deploys = new V1DeploymentList();
        try {
            deploys = api.listNamespacedDeployment(namespace, false, null, null, null, "app=tweet-filter", null, null, null, false);
        } catch (ApiException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        List<Filter> filters = new ArrayList<>();
        for (V1Deployment deployment : deploys.getItems()) {
            try {
                Filter filter = new Filter(deployment);
                filter.setUrl(uriInfo.getRequestUriBuilder().path(filter.getEventName()).build());
                filters.add(filter);
            } catch (IllegalStateException e) {
                logger.info("Ignoring this deployment");
            }
        }
        return filters;
    }


    @GET
    @Path("{event_name}")
    public Filter fetchOne(@Context UriInfo uriInfo, @PathParam("event_name") String eventName) {

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
            filter.setUrl(uriInfo.getRequestUri());
            return filter;
        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Path("{event_name}")
    public Response deleteOne(@Context UriInfo uriInfo, @PathParam("event_name") String eventName) {

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        try {

            api.deleteNamespacedDeployment(Filter.toDeploymentName(eventName), namespace, new V1DeleteOptions(),null,null,null,null,null);
            return Response.noContent().build();
        } catch (ApiException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

    }


    @POST
    public Response create(@NotNull @Valid Filter filter, @Context UriInfo uriInfo) {
        V1Deployment deploy = filter.toDeployment(kafkaServers, tweetStoreVersion);
        filter.setUrl(uriInfo.getRequestUriBuilder().path(filter.getEventName()).build());

        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        try {
            api.createNamespacedDeployment(namespace, deploy, false, null, null);
            return Response.created(filter.getUrl()).entity(filter).build();
        } catch (ApiException e) {
            logger.info("Already existing deployment");
        }
        try {
            api.replaceNamespacedDeployment(filter.deployName(), namespace, deploy, null, null);
            return Response.created(filter.getUrl()).entity(filter).build();
        } catch (ApiException e) {
            logger.info("Already existing deployment");
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.NOT_MODIFIED);
        }
    }


}
