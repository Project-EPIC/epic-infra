package edu.colorado.cs.epic.tweetsapi.resource;

import edu.colorado.cs.epic.tweetsapi.api.StorageIndex;
import edu.colorado.cs.epic.tweetsapi.core.EventDateCount;
import io.dropwizard.jersey.params.LongParam;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.*;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/tweets/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class TweetResource {

    private final Logger logger;
    private final StorageIndex storageIndex;

    public TweetResource(StorageIndex storageIndex) {
        this.logger = Logger.getLogger(TweetResource.class.getName());
        this.storageIndex = storageIndex;
    }

    @GET
    @Path("/{eventName}/")
    public String getTweets(@PathParam("eventName") String eventName,
                            @QueryParam("page") @DefaultValue("1") @Min(1) LongParam page,
                            @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) LongParam pageCount) {
        long pageNumber = page.get();
        long pageSize = pageCount.get();

        long startIndex = (pageNumber - 1) * pageSize;
        long endIndex = startIndex + pageSize;

        // Load in 'new' tweets for this event into the event's index
        storageIndex.updateEventIndex(eventName);

        // Check if we have tweets on event
        long totalCount = storageIndex.getEventTweetTotal(eventName);

        if (totalCount == -1) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else if (totalCount < startIndex) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        StringBuilder tweets = new StringBuilder();
        tweets.append("{\"tweets\":[");
        long numTweets = pageSize;
        try {
            tweets.append(storageIndex.getPaginatedTweets(eventName, startIndex, endIndex));
        } catch (ParseException | IOException e) {
            logger.error("Issue parsing JSON", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        tweets.append("],");

        JSONObject metaObject = new JSONObject();
        metaObject.put("count", pageSize);
        metaObject.put("total_count", totalCount);
        metaObject.put("num_pages", (long) Math.ceil((double) totalCount / pageSize));
        metaObject.put("page", pageNumber);
        metaObject.put("event_name", eventName);
        metaObject.put("tweet_count", numTweets);

        tweets.append("\"meta\":");
        tweets.append(metaObject.toJSONString());
        tweets.append("}");

        return tweets.toString();
    }

    protected enum AggregationBucket {
        hour,
        day,
        month
    }

    @GET
    @Path("{eventName}/counts")
    public JSONObject eventCount(@PathParam("eventName") String eventName, @DefaultValue("hour") @QueryParam("bucket") AggregationBucket bucket) {

        // Load in 'new' tweets for this event into the event's index
        storageIndex.updateEventIndex(eventName);

        // Check if we have tweets on event
        long totalCount = storageIndex.getEventTweetTotal(eventName);

        if (totalCount == -1) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        String dateFormat = "yyyy-MM-dd HH24:00:00";
        switch (bucket) {
            case day:
                dateFormat = "yyyy-MM-dd 00:00:00";
                break;
            case month:
                dateFormat = "yyyy-MM-00 00:00:00";
                break;
            default:
                break;
        }

        List<EventDateCount> counts = storageIndex.getEventCounts(eventName, dateFormat);
        List<JSONObject> countObjects = new ArrayList<>();
        for (EventDateCount item : counts) {
            JSONObject object = new JSONObject();
            object.put("time", item.getDateStr());
            object.put("count", item.getCount());
            countObjects.add(object);
        }


        JSONObject meta = new JSONObject();
        meta.put("event_name", eventName);
        meta.put("bucket", bucket);

        JSONObject result = new JSONObject();
        result.put("tweets", countObjects);
        result.put("meta", meta);

        return result;

    }
}
