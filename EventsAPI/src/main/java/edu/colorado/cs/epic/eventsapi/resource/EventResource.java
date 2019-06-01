package edu.colorado.cs.epic.eventsapi.resource;

import com.google.common.collect.ImmutableMultimap;
import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.api.ExtendedEvent;
import edu.colorado.cs.epic.eventsapi.core.BigQueryController;
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

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class EventResource {
    private final Logger logger;
    private DatabaseController controller;
    private BigQueryController bqController;
    private SyncEventsTask syncTask;

    public EventResource(DatabaseController controller, BigQueryController bqController, SyncEventsTask syncTask) {
        this.controller = controller;

        this.syncTask = syncTask;
        this.bqController = bqController;
        this.logger = Logger.getLogger(EventResource.class.getName());
    }

    @POST
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo, @Auth Optional<FirebaseUser> user) {

        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        event.setStatus(Event.Status.ACTIVE);

        user.ifPresent(firebaseUser -> event.setAuthor(firebaseUser.getEmail()));

        if (controller.eventExists(event.getNormalizedName())) {
            logger.info(String.format("Already existing event: %s", event.getNormalizedName()));
            throw new WebApplicationException(Response.Status.CONFLICT);
        }


        controller.insertEvent(event);
        syncInfrastructure(user);

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
        ExtendedEvent event;
        try {
            event = controller.getEvent(normalized_name);
            event.setStatus(status);
            controller.setStatus(normalized_name, status);

        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        syncInfrastructure(user);
        event = controller.getEvent(normalized_name);
        return Response.accepted(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
    }

    @PUT
    @Path("/big_query/{normalized_name}/")
    public ExtendedEvent createBigQueryTable(@PathParam("normalized_name") String normalizedName) {
        ExtendedEvent event;
        try {
            event = controller.getEvent(normalizedName);
            bqController.createBigQueryTable(event);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        event.setBigQueryTableURL(bqController.getEventTableURL(event));
        return event;
    }

    private void syncInfrastructure(Optional<FirebaseUser> user) {
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
    }


}