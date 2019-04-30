package edu.colorado.cs.epic.eventsapi.resource;

import com.google.cloud.bigquery.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.MultimapBuilder;
import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.api.ExtendedEvent;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.tasks.SyncEventsTask;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;

import java.util.List;
import java.util.Optional;

import io.dropwizard.auth.Auth;
import org.apache.log4j.Logger;

import static com.google.api.client.util.Charsets.UTF_8;

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class EventResource {
    private final Logger logger;
    private DatabaseController controller;
    private SyncEventsTask syncTask;

    public EventResource(DatabaseController controller, SyncEventsTask syncTask) {
        this.controller = controller;
        this.syncTask = syncTask;

        this.logger = Logger.getLogger(EventResource.class.getName());
    }

    @POST
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo, @Auth Optional<FirebaseUser> user) {

        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        event.setStatus(Event.Status.ACTIVE);

        if (user.isPresent()) {
            event.setAuthor(user.get().getEmail());
        }

        if (controller.eventExists(event.getNormalizedName())) {
            logger.info(String.format("Already existing event: %s", event.getNormalizedName()));
            throw new WebApplicationException(Response.Status.CONFLICT);
        }


        controller.insertEvent(event);


        // Create empty file to create folder structure
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of("bucket", event.getNormalizedName() + "/_EMPTY");
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
        storage.create(blobInfo, "".getBytes(UTF_8));

        // Create big query table
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
        TableId t = TableId.of("tweets", event.getNormalizedName());
        ExternalTableDefinition x = ExternalTableDefinition.newBuilder("gs://epic-collect/" + event.getNormalizedName() + "/*", null, FormatOptions.json()).setMaxBadRecords(Integer.MAX_VALUE).setIgnoreUnknownValues(true).setCompression("GZIP").setAutodetect(true).build();
        TableInfo k = TableInfo.newBuilder(t, x).build();
        bigquery.create(k, BigQuery.TableOption.fields(BigQuery.TableField.EXTERNAL_DATA_CONFIGURATION));


        try {
            ImmutableMultimap<String, String> map = new ImmutableMultimap.Builder<String, String>().build();
            if (user.isPresent()) {
                map = new ImmutableMultimap.Builder<String, String>()
                        .put("author", user.get().getEmail())
                        .build();
            }
            syncTask.execute(null, null);
        } catch (Exception e) {
            logger.error("Failed at sync with Kubernetes", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        return Response.created(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
    }


    @GET
    public List<Event> getEvents() {
        return controller.getEvents();
    }


    @GET
    @Path("/{normalized_name}")
    public ExtendedEvent getEvent(@PathParam("normalized_name") String normalized_name) {
        try {
            return controller.getEvent(normalized_name);

        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @PUT
    @Path("/{id}/{status}")
    public Response setStatus(@PathParam("id") String normalized_name, @PathParam("status") Event.Status status, @Context UriInfo uriInfo, @Auth Optional<FirebaseUser> user) {
        Event event;
        try {
            event = controller.getEvent(normalized_name);
            event.setStatus(status);
            controller.setStatus(normalized_name, status);

        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        try {

            ImmutableMultimap<String, String> map = new ImmutableMultimap.Builder<String, String>().build();
            if (user.isPresent()) {
                map = new ImmutableMultimap.Builder<String, String>()
                        .put("author", user.get().getEmail())
                        .build();
            }

            syncTask.execute(map, null);
        } catch (Exception e) {
            logger.error("Failed at sync with Kubernetes", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        event = controller.getEvent(normalized_name);
        return Response.accepted(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
    }


}