package eventapi.resource;

import eventapi.api.Event;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.postgresql.util.PSQLException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {
    private Jdbi postgres;

    public EventResource(Jdbi postgres) {
        this.postgres = postgres;
    }

    @POST
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo) {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"

        try {
            postgres.withHandle(handle -> {
                handle.createUpdate("INSERT INTO events (name, description, normalized_name, status, created_at) VALUES (:name,:description,:normalizedName,:status,:createdAt)")
                        .bindBean(event)
                        .execute();


                PreparedBatch batch = handle.prepareBatch("INSERT INTO keywords (event_name, keyword) VALUES (:name,:keyword)");
                for (String keyword : event.getKeywords()) {
                    batch.bind("keyword", keyword).bind("name", event.getNormalizedName()).add();
                }
                return batch.execute();
            });
        } catch (UnableToExecuteStatementException e) {
            Event oldEvent = postgres.withHandle(handle ->
            {
                Event internal = handle
                        .createQuery("SELECT * FROM events WHERE normalized_name=:normalizedName ORDER BY normalized_name")
                        .bind("normalizedName", event.getNormalizedName())
                        .mapToBean(Event.class)
                        .findOnly();
                internal.setKeywords(handle.createQuery("SELECT keyword FROM keywords WHERE event_name=:normalizedName ORDER BY keyword")
                        .bind("normalizedName", event.getNormalizedName())
                        .mapTo(String.class)
                        .list());
                return internal;
            });


            return Response.status(Response.Status.CONFLICT).contentLocation(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build())
                    .entity(oldEvent)
                    .build();
        }


        return Response.created(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
    }

    @GET
    public List<Event> getEvents() {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"

        return postgres.withHandle(handle -> {
            List<Event> events = handle.createQuery("SELECT * FROM events ORDER BY normalized_name")
                    .mapToBean(Event.class)
                    .list();
            for (Event event : events) {
                event.setKeywords(handle.createQuery("SELECT keyword FROM keywords WHERE event_name=:normalizedName ORDER BY keyword")
                        .bind("normalizedName", event.getNormalizedName())
                        .mapTo(String.class)
                        .list());
            }
            return events;
        });

    }


    @GET
    @Path("/{normalized_name}")
    public Event getEvent(@PathParam("normalized_name") String normalized_name) {
        try {
            return postgres.withHandle(handle -> {
                Event event = handle.createQuery("SELECT * FROM events WHERE normalized_name=:normalizedName ORDER BY normalized_name")
                        .bind("normalizedName", normalized_name)
                        .mapToBean(Event.class)
                        .findOnly();
                event.setKeywords(handle.createQuery("SELECT keyword FROM keywords WHERE event_name=:normalizedName ORDER BY keyword")
                        .bind("normalizedName", event.getNormalizedName())
                        .mapTo(String.class)
                        .list());
                return event;

            });
        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Path("/{normalized_name}")
    public Event deleteEvent(@PathParam("normalized_name") String normalized_name) {
        try {
            return postgres.withHandle(handle -> handle
                    .createQuery("SELECT * FROM events WHERE normalized_name=:normalizedName ORDER BY normalized_name")
                    .bind("normalizedName", normalized_name)
                    .mapToBean(Event.class)
                    .findOnly()
            );
        } catch (IllegalStateException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @PUT
    @Path("/{id}/{status}")
    public Response setStatus(@PathParam("id") String normalized_name, @PathParam("status") String status) throws SQLException {
//        Connection conn = postgres.getConnection();
//        String sql = "UPDATE events set status = '" + status + "' where normalized_name='" + normalized_name + "';";
//        Statement stmt = conn.createStatement();
//        System.out.println(sql);
//        stmt.execute(sql);
//        return Response.ok().build();
        throw new WebApplicationException(Response.Status.NOT_IMPLEMENTED);
    }
}