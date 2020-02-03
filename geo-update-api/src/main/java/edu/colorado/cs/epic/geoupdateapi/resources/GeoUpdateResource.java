package edu.colorado.cs.epic.geoupdateapi.resources;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.OrderedJSONObject;
import edu.colorado.cs.epic.GeoUpdateLib;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Path("/geoupdate/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class GeoUpdateResource {
  private final Logger logger;
  private Map<String, String> inProgress = new ConcurrentHashMap<String, String>();

  // We will keep track of what jobs are running in memory... This will not scale
  // if we have many replicas, but ideally this service will not be used much
  private ThreadPoolExecutor executor;

  public GeoUpdateResource(ThreadPoolExecutor executor) {
    this.logger = Logger.getLogger(GeoUpdateResource.class.getName());
    this.executor = executor;
  }

  @POST
  @Path("/{eventName}")
  public Response updateEvent(@PathParam("eventName") String eventName,
      @QueryParam("srcbucket") @Size(min = 1) String srcbucket,
      @QueryParam("destbucket") @Size(min = 1) String destbucket) throws InterruptedException {

    String geoUpdateRequestKey = String.format("%s,%s,%s", eventName, srcbucket, destbucket);

    if (inProgress.get(geoUpdateRequestKey) != null) {
      // This job is already running, so do not start the same one
      return Response.status(409).build();
    }

    // Add this to list of current jobs being executed
    inProgress.put(geoUpdateRequestKey, "");

    executor.execute(() -> {
      GeoUpdateLib.asynEventUpdate(eventName, srcbucket, destbucket);
      inProgress.remove(geoUpdateRequestKey);
    });

    return Response.status(202).build();
  }

  @GET
  @Path("/status/{eventName}")
  public String jobStatuses(@PathParam("eventName") String eventName) throws InterruptedException {

    Set<String> keys = inProgress.keySet();

    ArrayList<String> runningJobs = new ArrayList<String>();
    for (String key : keys) {
      if (key.contains(eventName)) {
        String[] geoUpdateRequest = key.split(",");

        // Prepare and append meta data object for event
        OrderedJSONObject metaObject = new OrderedJSONObject();
        try {
          metaObject.put("event_name", eventName);
          metaObject.put("srcbucket", geoUpdateRequest[1]);
          metaObject.put("destbucket", geoUpdateRequest[2]);
        } catch (JSONException e) {
          e.printStackTrace();
        }
        runningJobs.add(metaObject.toString());
      }
    }
    StringBuilder result = new StringBuilder();
    result.append("{\"result\":");
    result.append(runningJobs.toString());
    result.append("}");

    return result.toString();
  }
}