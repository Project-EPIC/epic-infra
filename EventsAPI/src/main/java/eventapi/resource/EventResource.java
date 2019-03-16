package eventapi.resource;

import eventapi.api.Event;
import net.dongliu.requests.Parameter;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

@Path("/events/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {
    private final String kubernetesFilterApiUrl;
    private final Logger logger;
    private Jdbi postgres;

    public EventResource(Jdbi postgres, String kubernetesFilterApiUrl) {
        this.postgres = postgres;
        this.kubernetesFilterApiUrl = kubernetesFilterApiUrl;
        this.logger = Logger.getLogger(EventResource.class.getName());
    }

    @POST
    public Response createEvent(@NotNull @Valid Event event, @Context UriInfo uriInfo) {
        // Check if there's any event with the name
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

        if(updateLowLevelAPI(event,event.getStatus())){
            try {
                // Update DB with event and keywords
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
                return Response.created(uriInfo.getRequestUriBuilder().path(event.getNormalizedName()).build()).entity(event).build();
            } catch (UnableToExecuteStatementException e) {
                // If DB fails, delete from API and throw that we are unavailable
                logger.warning("Something went terribly wrong. Rolling back!");
                e.printStackTrace();
                RawResponse delResp  = Requests.delete(kubernetesFilterApiUrl + event.getNormalizedName()).send();
                if (delResp.statusCode() != Response.Status.NO_CONTENT.getStatusCode()) {
                    logger.warning(String.format("Kubernetes API returned code when deleting: %d", delResp.statusCode()));
                }
                throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            }
        }else{
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

    }

    @GET
    public List<Event> getEvents() {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"

        return postgres.withHandle(handle -> new ArrayList<>(handle.createQuery(
                "SELECT e.name e_name, e.normalized_name e_norm_name, e.description e_desc, e.status e_status, e.created_at e_created, k.keyword k_key " +
                        "FROM events e INNER JOIN keywords k ON k.event_name = e.normalized_name " +
                        "ORDER BY e.normalized_name")
                .registerRowMapper(BeanMapper.factory(Event.class, "e"))
                .registerRowMapper(BeanMapper.factory(String.class, "k"))
                .reduceRows(new LinkedHashMap<String, Event>(),
                        (map, rowView) -> {
                            Event event = map.computeIfAbsent(
                                    rowView.getColumn("e_norm_name", String.class),
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
        Event event=getEvent(normalized_name);

        if(updateLowLevelAPI(event,status)){
            //Update the event
            try {
                postgres.withHandle(handle -> {
                    handle.createUpdate("UPDATE events set status=:staus where normalized_name:normalizedName")
                            .bind("normalizedName", normalized_name)
                            .bind("staus", status)
                            .execute();
                    return 1;
                });
            }catch (UnableToExecuteStatementException ex){
                throw  new WebApplicationException(Response.Status.BAD_REQUEST);
            }

        }
        return Response.ok().entity(getEvent(normalized_name)).build();
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
        RawResponse response = Requests.post(kubernetesFilterApiUrl).jsonBody(getAllKeywords()).send();
        if (response.statusCode() != Response.Status.CREATED.getStatusCode()) {
            logger.warning(String.format("Kubernetes API returned code: %d", response.statusCode()));
            logger.warning("Body returned: "+ response.readToText());
            return false;
        }
        return true;
    }

    private boolean resetFilterAPI(Event event, String target_status) {

        if (target_status.equals("NOT_ACTIVE")) {
            RawResponse response = Requests.delete(kubernetesFilterApiUrl + event.getNormalizedName()).send();
            if (response.statusCode() != Response.Status.NO_CONTENT.getStatusCode()) {
                logger.warning(String.format("Kubernetes API returned code: %d", response.statusCode()));
                logger.warning("Body returned: " + response.readToText());
                return false;
            }
        } else {
            HashMap<String, Object> filterBody = new HashMap<>();
            filterBody.put("event_name", event.getNormalizedName());
            filterBody.put("keywords", event.getKeywords());
            RawResponse response = Requests.post(kubernetesFilterApiUrl)
                    .jsonBody(filterBody)
                    .send();
            if (response.statusCode() != Response.Status.CREATED.getStatusCode()) {
                logger.warning(String.format("Kubernetes API returned code: %d", response.statusCode()));
                logger.warning("Body returned: " + response.readToText());
                return false;
            }
        }
        return true;
    }


    private List<String> getAllKeywords(){
        return postgres.withHandle(handle -> {
            return handle.createQuery("SELECT distinct keyword from keywords")
                    .mapTo(String.class)
                    .list();
        });
    }
}