package eventapi.resource;

import eventapi.api.Event;
import eventapi.api.Filter;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {
    private final Logger logger;
    private Jdbi postgres;
    private QueryResource queryResource;
    private FilterResource filterResource;

    public EventResource(Jdbi postgres, QueryResource queryResource, FilterResource filterResource) {
        this.postgres = postgres;
        this.queryResource=queryResource;
        this.filterResource=filterResource;
        this.logger = Logger.getLogger(EventResource.class.getName());
    }

    @POST
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo) {
        // Check if there's any event with the name
        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        event.setStatus("ACTIVE");

        //Check if event is there
        try {
            Integer count = postgres.withHandle(handle -> handle.createQuery("SELECT count(*) FROM events WHERE normalized_name=:normalizedName")
                    .bind("normalizedName", event.getNormalizedName())
                    .mapTo(Integer.class)
                    .findOnly()
            );
            if (count > 0) {
                // If exists, return 409 Conflict
                logger.info(String.format("Already existing event: %s (%d)", event.getNormalizedName(), count));
                throw new WebApplicationException(Response.Status.CONFLICT);
            }
        } catch (UnableToExecuteStatementException e) {
            // Something went terribly wrong LOL
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        // Update DB with event and keywords
        try {
            postgres.withHandle(handle -> {
                handle.createUpdate("INSERT INTO events (name, description, normalized_name, status, created_at) VALUES (:name,:description,:normalizedName,:status,:createdAt)")
                        .bindBean(event)
                        .execute();
                PreparedBatch batch = handle.prepareBatch("INSERT INTO keywords (event_name, keyword) VALUES (:name, :keyword)");
                for (String keyword : event.getKeywords()) {
                    batch.bind("keyword", keyword)
                            .bind("name", event.getNormalizedName())
                            .add();
                }
                return batch.execute();
            });

        } catch (UnableToExecuteStatementException e) {
            // If DB fails, throw that we are unavailable
            logger.warning("Something went terribly wrong in the database.");
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        //Update the API
        // Todo: Change this to exeception!
        if(updateLowLevelAPI(event,event.getStatus())){
            return Response.created(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
        }else{
            // Todo: Change this to just mark event status as failed.
            //If update fails roll back the DB
            logger.warning("Something went terribly wrong in the kubernetes api. Rolling back the databse");
            try {
                postgres.useHandle(handle -> {
                    handle.createUpdate("DELETE FROM keywords WHERE event_name=:normalized_name")
                            .bind("normalized_name",event.getNormalizedName())
                            .execute();
                    handle.createUpdate("DELETE FROM events WHERE normalized_name=:normalized_name")
                            .bind("normalized_name", event.getNormalizedName())
                            .execute();

                });
                throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            } catch (UnableToExecuteStatementException e) {
                // If DB fails, We have serious issues
                logger.warning("Something went terribly wrong in the database while rolling back updae. Database not in consistent position");
                e.printStackTrace();
                throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            }
        }
    }

    @GET
    public List<Event> getEvents() {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"

        return postgres.withHandle(handle -> new ArrayList<>(handle.createQuery(
                "SELECT e.name e_name, e.normalized_name e_normalized_name, e.description e_description, e.status e_status, e.created_at e_created_at, k.keyword k_key " +
                        "FROM events e INNER JOIN keywords k ON k.event_name = e.normalized_name " +
                        "ORDER BY e.normalized_name")
                .registerRowMapper(BeanMapper.factory(Event.class, "e"))
                .registerRowMapper(BeanMapper.factory(String.class, "k"))
                .reduceRows(new LinkedHashMap<String, Event>(),
                        (map, rowView) -> {
                            Event event = map.computeIfAbsent(
                                    rowView.getColumn("e_normalized_name", String.class),
                                    id -> rowView.getRow(Event.class)
                            );
                            event.appendKeywords(rowView.getColumn("k_key", String.class));
                            return map;
                        })
                .values()
        ));

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

    @PUT
    @Path("/{id}/{status}")
    public Response setStatus(@PathParam("id") String normalized_name, @PathParam("status") String status, @Context UriInfo uriInfo)  {
        // Check if there's any event with the name
        // Todo: Check status is allowed (change it to be enumeration maybe?)
        Event event=getEvent(normalized_name);

        //Update the event
        try {
            postgres.withHandle(handle -> {
                handle.createUpdate("UPDATE events set status=:staus where normalized_name=:normalizedName")
                        .bind("normalizedName", normalized_name)
                        .bind("staus", status)
                        .execute();
                return 1;
            });
        }catch (UnableToExecuteStatementException ex){
            throw  new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        if(updateLowLevelAPI(event,status)) {
            return Response.accepted().entity(getEvent(normalized_name)).build();
        }else{
            //Update the event
            try {
                postgres.withHandle(handle -> {
                    handle.createUpdate("UPDATE events set status=:staus where normalized_name=:normalizedName")
                            .bind("normalizedName", normalized_name)
                            .bind("staus", event.getStatus())
                            .execute();
                    return 1;
                });
            }catch (UnableToExecuteStatementException ex){
                logger.warning("Database update failed after the kubernetes api failed");
                throw  new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            }
            throw  new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }


    private boolean updateLowLevelAPI(Event event, String target_status){
        if(resetFilterAPI(event,target_status)){
            if(resetQueryAPI()){
                return true;
            }else{
                resetFilterAPI(event,target_status.equals("ACTIVE") ? "NOT_ACTIVE":"ACTIVE");
                return false;
            }
        }else {
            return  false;
        }
    }

    private boolean resetQueryAPI() {
        return queryResource.update(getAllActiveKeywords());
    }

    private boolean resetFilterAPI(Event event, String target_status) {

        if (target_status.equals("NOT_ACTIVE")) {
            return filterResource.deleteOne(event.getNormalizedName());
        } else {
            Filter f=new Filter(event.getKeywords(), event.getNormalizedName());
            return filterResource.create(f);
        }
    }


    private List<String> getAllActiveKeywords(){
        return postgres.withHandle(handle -> {
            return handle.createQuery("select DISTINCT keyword from keywords,events where event_name=name and status='ACTIVE'")
                    .mapTo(String.class)
                    .list();
        });
    }
}