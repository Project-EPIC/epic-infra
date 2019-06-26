package edu.colorado.cs.epic.tweetsapi.resource;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.colorado.cs.epic.tweetsapi.api.EventIndex;
import io.dropwizard.jersey.params.DateTimeParam;
import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.text.SimpleDateFormat;
import java.util.*;


import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Path("/tweets/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class TweetResource {

    private final Logger logger;
    private final LoadingCache<String, EventIndex> filesCache;

    public TweetResource(Bucket storage) {
        this.logger = Logger.getLogger(TweetResource.class.getName());
        filesCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, EventIndex>() {
                    @Override
                    public EventIndex load(String key) {
                        Page<Blob> blobs = storage.list(Storage.BlobListOption.prefix(key + "/"), Storage.BlobListOption.fields(Storage.BlobField.NAME));

                        EventIndex index = new EventIndex(new Date());
                        int current = 0;
                        for (Blob blob : blobs.iterateAll()) {
                            if (blob.getName().contains(".json.gz")) {
                                String nameSpilt = blob.getName().replace(".json.gz", "");
                                String[] fileDetails = nameSpilt.split("-");
                                int size = Integer.parseInt(fileDetails[fileDetails.length - 1]);
                                index.addItem(new EventIndex.Item(blob, current, size));
                                current += size;
                            }
                        }
                        return index;
                    }
                });
    }

    @GET
    @Path("/{eventName}/")
    public String getTweets(@PathParam("eventName") String eventName,
                            @QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
                            @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount,
                            @QueryParam("since") DateTimeParam sinceParam, @QueryParam("until") DateTimeParam untilParam) {
        int pageNumber = page.get();
        int pageSize = pageCount.get();

        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = startIndex + pageSize;
        List<EventIndex.Item> indexList;
        Date updateTime;
        try {
            EventIndex index = filesCache.get(eventName);
            indexList = index.getIndex();
            updateTime = index.getUpdateTime();
        } catch (ExecutionException e) {
            logger.error("Issue accessing Google Cloud", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }


        indexList = filterByDate(indexList, sinceParam, untilParam);

        // Check if we have tweets on event
        if (indexList.size() == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // Check if page has any information
        EventIndex.Item lastEvent = indexList.get(indexList.size() - 1);
        int totalCount = lastEvent.getIndex() + lastEvent.getSize();
        if (totalCount < startIndex) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        int fileIndex = floorSearch(indexList, 0, indexList.size() - 1, startIndex);
        StringBuilder tweets = new StringBuilder();
        tweets.append("{\"tweets\":[");
        int numTweets = pageSize;
        try {
            if (fileIndex == (indexList.size() - 1)) {
                EventIndex.Item item = indexList.get(fileIndex);
                tweets.append(item.getData(startIndex, endIndex));
                numTweets = Math.min((item.getIndex() + item.getSize()) - startIndex, pageSize);
            } else if (!(startIndex + pageSize > indexList.get(fileIndex + 1).getIndex())) {
                tweets.append(indexList.get(fileIndex).getData(startIndex, endIndex));
            } else {
                tweets.append(indexList.get(fileIndex).getData(startIndex, endIndex));
                tweets.append(",");
                tweets.append(indexList.get(fileIndex + 1).getData(startIndex, endIndex));
            }
        } catch (ParseException | IOException e) {
            logger.error("Issue parsing JSON", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
        tweets.append("],");

        JSONObject metaObject = new JSONObject();
        metaObject.put("count", pageSize);
        metaObject.put("total_count", totalCount);
        metaObject.put("num_pages", (int) Math.ceil((double) totalCount / pageSize));
        metaObject.put("page", pageNumber);
        metaObject.put("event_name", eventName);
        metaObject.put("tweet_count", numTweets);
        metaObject.put("refreshed_time", updateTime.toString());

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

        EventIndex index;
        try {
            index = filesCache.get(eventName);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }


        // Check if we have tweets on event
        if (index.getIndex().size() == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:00:00Z");
        switch (bucket) {
            case day:
                parser = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00Z");
                break;
            case month:
                parser = new SimpleDateFormat("yyyy-MM-01'T'00:00:00Z");
                break;
            default:
                break;
        }


        SimpleDateFormat finalParser = parser;
        Map<String, Integer> counts = index.getIndex().stream()
                .collect(Collectors.groupingBy(
                        // Get string to group by
                        item -> finalParser.format(item.getDate()),
                        // Creator for returning class
                        TreeMap::new,
                        // How to sum for each item
                        Collectors.summingInt(EventIndex.Item::getSize)
                ));
        List<JSONObject> countObjects = new ArrayList<>();
        for (Map.Entry<String, Integer> item : counts.entrySet()) {
            JSONObject object = new JSONObject();
            object.put("time", item.getKey());
            object.put("count", item.getValue());
            countObjects.add(object);
        }


        JSONObject meta = new JSONObject();
        meta.put("event_name", eventName);
        meta.put("bucket", bucket);
        meta.put("refreshed_time", index.getUpdateTime().toString());

        JSONObject result = new JSONObject();
        result.put("tweets", countObjects);
        result.put("meta", meta);

        return result;

    }

    private List<EventIndex.Item> filterByDate(List<EventIndex.Item> index, DateTimeParam sinceParam, DateTimeParam untilParam) {
        if (sinceParam == null && untilParam == null) {
            return index;
        }
        DateTime since = sinceParam == null ? null : sinceParam.get();
        DateTime until = untilParam == null ? null : untilParam.get();
        List<EventIndex.Item> filteredIndex = index.stream()
                .filter((item -> {
                    if (since != null && until != null)
                        return until.isAfter(item.getDate().getTime()) && since.isBefore(item.getDate().getTime());
                    else if (since != null)
                        return since.isBefore(item.getDate().getTime());
                    else
                        return until.isAfter(item.getDate().getTime());
                }))
                .collect(Collectors.toList());

        // If empty return empty
        if (filteredIndex.isEmpty()) {
            return filteredIndex;
        }

        // Update index
        int initialIndex = filteredIndex.get(0).getIndex();
        return filteredIndex.stream()
                .map(EventIndex.Item::new)
                .map((item -> item.setIndex(item.getIndex() - initialIndex)))
                .collect(Collectors.toList());
    }

    private int floorSearch(List<EventIndex.Item> arr, int low, int high, int index) {
        if (low > high)
            return -1;
        if (index >= arr.get(high).getIndex())
            return high;
        int mid = (low + high) / 2;

        if (arr.get(mid).getIndex() == index)
            return mid;

        if (mid > 0 && arr.get(mid - 1).getIndex() <= index && index < arr.get(mid).getIndex())
            return mid - 1;

        if (index < arr.get(mid).getIndex())
            return floorSearch(arr, low, mid - 1, index);
        else
            return floorSearch(arr, mid + 1, high, index);
    }
}
