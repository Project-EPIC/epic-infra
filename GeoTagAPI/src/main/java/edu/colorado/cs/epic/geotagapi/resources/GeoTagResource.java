package edu.colorado.cs.epic.geotagapi.resources;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.common.collect.Iterables;

import org.apache.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.colorado.cs.epic.geotagapi.core.GeoTagIndexRow;
import edu.colorado.cs.epic.geotagapi.jdbi3.EventDAO;
import edu.colorado.cs.epic.geotagapi.jdbi3.GeoTagIndexDAO;
import io.dropwizard.jersey.params.LongParam;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/geotag/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeoTagResource {
    private final Logger logger;
    private final Bucket bucket;
    private final Jdbi jdbi;
    private final int BUFFER_SIZE = 1000;
    final String TWITTER_DATE_FORMAT="EEE MMM dd HH:mm:ss ZZZZZ yyyy";

    public GeoTagResource(Bucket storage, Jdbi jdbi) {
        this.logger = Logger.getLogger(GeoTagResource.class.getName());
        bucket = storage;

        jdbi.registerRowMapper(GeoTagIndexRow.class, (rs, ctx) -> 
            new GeoTagIndexRow(rs.getString("quad_key"), rs.getString("created_at"), rs.getString("username"), rs.getString("user_id_str"),
                            rs.getString("tweet_id_str"), rs.getString("lang"), rs.getString("source"), rs.getString("in_reply_to_user_id_str"),
                            rs.getString("text"), rs.getString("image_link"), rs.getBoolean("is_retweet")));
        this.jdbi = jdbi;
    }

    @GET
    @Path("/{eventName}/")
    @RolesAllowed("ADMIN")
    public void getTweets(@PathParam("eventName") String eventName,
                            @QueryParam("resolution") @DefaultValue("1") @Min(4) @Max(8) LongParam resolution) {
        logger.info("HELLO WORLD");
        return;
    }

    @POST
    @Path("/{eventName}/")
    public void createGeoTagIndex(@PathParam("eventName") String eventName) throws IOException {
        // Blob path including the event name
        Page<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(String.format("spark/geotag/%s/", eventName)), Storage.BlobListOption.fields(Storage.BlobField.NAME));
        
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
        ByteArrayInputStream bais = new ByteArrayInputStream(lastBlob.getContent());
        GZIPInputStream gzis = new GZIPInputStream(bais);
        InputStreamReader input = new InputStreamReader(gzis);
        BufferedReader reader = new BufferedReader(input);

        JSONParser parser = new JSONParser();

        SimpleDateFormat dateFormat = new SimpleDateFormat(TWITTER_DATE_FORMAT, Locale.getDefault());
        dateFormat.setLenient(true);

        final List<GeoTagIndexRow> newRows = new ArrayList<>();
        while (reader.ready()) {
            try {
                String line = reader.readLine();
                JSONObject json = (JSONObject) parser.parse(line);

                newRows.add(new GeoTagIndexRow((String)json.get("quad_key"), (String)json.get("created_at"), 
                (String)json.get("user"),  (String)json.get("user_id_str"),(String) json.get("tweet_id_str"), 
                (String)json.get("lang"), (String)json.get("source"), (String)json.get("in_reply_to_user_id_str"), 
                (String)json.get("text"), (String)json.get("image_link"), json.containsKey("retweeted_status")));

                if (newRows.size() == BUFFER_SIZE || reader.ready() == false) {
                    jdbi.useHandle(handle -> {
                        EventDAO eventDAO = handle.attach(EventDAO.class);
                        GeoTagIndexDAO eventIndexDAO = handle.attach(GeoTagIndexDAO.class);

                        eventDAO.insertEvent(eventName);
                        int eventId = eventDAO.getEventId(eventName);

                        eventIndexDAO.bulkInsertGeoTagIndexEntries(eventId, newRows);
                    });
                    newRows.clear();
                }

            } catch (ParseException e) {
                logger.error(e);
            }
        }
    }
}
