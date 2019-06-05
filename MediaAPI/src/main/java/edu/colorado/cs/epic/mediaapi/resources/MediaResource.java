package edu.colorado.cs.epic.mediaapi.resources;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.Iterables;
import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.NoSuchElementException;


@Path("/media/")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MediaResource {
    private final Logger logger;
    private final Bucket bucket;



    public MediaResource(Bucket storage) {
        this.logger = Logger.getLogger(MediaResource.class.getName());
        bucket = storage;
    }


    @GET
    @Path("/{eventName}/")
    public String getMediaFromEvent(@PathParam("eventName") String eventName,
                                       @QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
                                       @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount) {

        // Number of pages we want to show
        int pageNumber = page.get();
        // Size of the pages
        int pageSize = pageCount.get();

        int startIndex = (pageNumber - 1) * pageSize;

        // Blob path including the event name
        Page<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(String.format("spark/media/%s/", eventName)), Storage.BlobListOption.fields(Storage.BlobField.NAME));

        // Iterate through all the blobs inside "epic-analysis-results/spark"
        Blob lastBlob;
        try {
            lastBlob = Iterables.getLast(blobs.iterateAll());
            if (lastBlob == null){
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } catch (NoSuchElementException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }


        // Store the content of the JSON file
        ByteArrayInputStream bais = new ByteArrayInputStream(lastBlob.getContent());

        InputStreamReader reader = new InputStreamReader(bais);
        LineNumberReader in = new LineNumberReader(reader);




        StringBuilder data = new StringBuilder();

        int returnedTweets = in.lines().skip(startIndex).limit(pageSize).map(tweet -> {
            data.append(tweet);
            data.append(",");
            return 1;
        }).reduce(Integer::sum).get();

        // Check if we have any data
        if (data.length() == 0) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Calculate total number of lines
        long remainingLines = in.lines().count();
        long totalCount = remainingLines + startIndex + returnedTweets;

        // remove last comma
        data.setLength(data.length() - 1);

        // Create the String that we will return
        StringBuilder tweets = new StringBuilder();
        // Format to proper JSON
        tweets.append("{\"media\":[");

        // Access the page we want to return
        tweets.append(data.toString());

        // Close our user tweets
        tweets.append("],");

        // Store some additional information

        JSONObject metaObject = new JSONObject();
        metaObject.put("count", pageSize);
        metaObject.put("page", pageNumber);
        metaObject.put("event_name", eventName);
        metaObject.put("total_count", totalCount);
        metaObject.put("tweet_count", returnedTweets);
        metaObject.put("num_pages", (int) Math.ceil((float)totalCount/pageSize));
        //metaObject.put("refreshed_time", updateTime.toString());

        // End the JSON formatting
        tweets.append("\"meta\":");
        tweets.append(metaObject.toJSONString());
        tweets.append("}");

        // Return the JSON string to the request
        return tweets.toString();

    }
}