package edu.colorado.cs.epic.geoupdateapi.resources;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.OrderedJSONObject;
import edu.colorado.cs.epic.GeoUpdateLib;

@Path("/geoupdate/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class GeoUpdateResource {
  private final Logger logger;

  public GeoUpdateResource() {
    this.logger = Logger.getLogger(GeoUpdateResource.class.getName());
  }

  @GET
  @Path("/{eventName}")
  public String updateEvent(@PathParam("eventName") String eventName,
      @QueryParam("srcbucket") @Size(min = 1) String srcbucket,
      @QueryParam("destbucket") @Size(min = 1) String destbucket) throws InterruptedException {

      int totalTweetCount = GeoUpdateLib.asynEventUpdate(eventName, srcbucket, destbucket);

      // Build the final result object
      StringBuilder result = new StringBuilder();
      result.append("{\"result\":");
      // Prepare and append meta data object
      OrderedJSONObject metaObject = new OrderedJSONObject();
      try {
        metaObject.put("event_name", eventName);
        metaObject.put("tweet_count", totalTweetCount);
        metaObject.put("srcbucket", srcbucket);
        metaObject.put("destbucket", destbucket);
        // metaObject.put("processing_time", ?);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      result.append(metaObject.toString());
      result.append("}");

      return result.toString();
  }
}