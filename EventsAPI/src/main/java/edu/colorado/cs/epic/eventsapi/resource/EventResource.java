package edu.colorado.cs.epic.eventsapi.resource;

import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.tasks.SyncEventsTask;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;

import java.util.List;

import org.apache.log4j.Logger;

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo) {
        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        event.setStatus(Event.Status.ACTIVE);

        if (controller.eventExists(event.getNormalizedName())) {
            logger.info(String.format("Already existing event: %s", event.getNormalizedName()));
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        controller.insertEvent(event);

        try {
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
    public Event getEvent(@PathParam("normalized_name") String normalized_name) {
        try {
            return controller.getEvent(normalized_name);

        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @PUT
    @Path("/{id}/{status}")
    public Response setStatus(@PathParam("id") String normalized_name, @PathParam("status") Event.Status status, @Context UriInfo uriInfo) {
        Event event;
        try {
            event = controller.getEvent(normalized_name);
            event.setStatus(status);
            controller.setStatus(normalized_name, status);

        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        try {
            syncTask.execute(null, null);
        } catch (Exception e) {
            logger.error("Failed at sync with Kubernetes", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        return Response.accepted(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
    }


}