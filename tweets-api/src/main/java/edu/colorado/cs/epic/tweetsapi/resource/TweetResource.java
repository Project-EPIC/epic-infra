package edu.colorado.cs.epic.tweetsapi.resource;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.google.common.collect.Streams;
import edu.colorado.cs.epic.tweetsapi.api.EventIndex;
import io.dropwizard.jersey.params.IntParam;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Path("/tweets/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TweetResource {
    private final Logger logger;
    private final Storage storage;
    private final LoadingCache<String, List<EventIndex>> filesCache;

    public TweetResource(Storage storage) {
        this.logger = Logger.getLogger(TweetResource.class.getName());
        this.storage = storage;
        filesCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, List<EventIndex>>() {
                    @Override
                    public List<EventIndex> load(String key) {
                        Page<Blob> blobs = storage.list("epic-collect", Storage.BlobListOption.prefix(key));

                        List<EventIndex> index = new ArrayList<>();
                        int current = 0;
                        for (Blob blob : blobs.iterateAll()) {
                            if (blob.getName().contains(".json.gz")) {
                                EventIndex e = new EventIndex(blob, current);
                                index.add(e);
                                current += e.getSize();
                            }
                        }
                        return index;
                    }
                });
    }

    @GET
    @Path("/{event_name}/")
    public List<JSONObject> getTweets(@PathParam("event_name") String event_name, @QueryParam("page") @DefaultValue("1") IntParam page, @QueryParam("size") @DefaultValue("100") IntParam size) throws ExecutionException, InterruptedException, IOException, ParseException {
        int pageNumber = page.get();
        int pageSize = size.get();

        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = startIndex + pageSize;
        List<EventIndex> indexList = filesCache.get(event_name);

        int fileIndex = floorSearch(indexList, 0, indexList.size() - 1, startIndex);
        List<JSONObject> data;
        if (!(startIndex + pageSize > indexList.get(fileIndex + 1).getIndex())) {
            data = indexList.get(fileIndex).getData(startIndex, endIndex);
        } else {
            data = indexList.get(fileIndex).getData(startIndex, endIndex);
            data.addAll(indexList.get(fileIndex + 1).getData(startIndex, endIndex));
        }
        return data;
    }

    private int floorSearch(List<EventIndex> arr, int low, int high, int index) {
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
