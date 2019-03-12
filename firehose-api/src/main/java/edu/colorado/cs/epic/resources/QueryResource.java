package edu.colorado.cs.epic.resources;

import com.google.gson.Gson;
import edu.colorado.cs.epic.api.Keywords;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.logging.Logger;


/**
 * Created by admin on 7/3/19.
 */

@Path("/query/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryResource {

    private final ApiClient client;
    private final String configMapNamespace;
    private final String configMapName;
    private final Logger logger;

    public QueryResource(ApiClient client, String configMapName, String configMapNamespace) {
        this.client = client;
        this.configMapName = configMapName;
        this.configMapNamespace = configMapNamespace;
        this.logger = Logger.getLogger(QueryResource.class.getName());
    }

    @GET
    public Keywords fetch() {
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        try {
            V1ConfigMap config = api.readNamespacedConfigMap(configMapName, configMapNamespace, null, true, false);
            String keywords = config.getData().getOrDefault("keywords", "");
            return new Keywords(Arrays.asList(keywords.split(",")));
        } catch (ApiException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

    }

    @POST
    public Response update(@NotNull @Valid Keywords keywords, @Context UriInfo uriInfo) {

        V1ConfigMap config = new V1ConfigMapBuilder()
                .addToData("keywords", String.join(",",keywords.getKeywords()))
                .editOrNewMetadata()
                .withName(configMapName)
                .withNamespace(configMapNamespace)
                .endMetadata()
                .build();

        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        try {
            api.createNamespacedConfigMap(configMapNamespace, config, false, null,null);
            return Response.created(uriInfo.getRequestUri()).entity(keywords).build();
        } catch (ApiException e) {
            logger.info("ConfigMap already existis. Replacing...");
        }

        try {

            api.replaceNamespacedConfigMap(configMapName, configMapNamespace, config, null, null);
            return Response.created(uriInfo.getRequestUri()).entity(keywords).build();
        } catch (ApiException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private Object deserialize(String jsonStr, Class<?> targetClass) {
        return (new Gson()).fromJson(jsonStr, targetClass);
    }
}
