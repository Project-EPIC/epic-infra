package edu.colorado.cs.epic.tweetsapi.resource;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.dropwizard.jersey.params.IntParam;

import java.util.*;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Path("/tweets/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TweetResource {
    private final Logger logger;
    private final LoadingCache<String, Page<Blob>> filesCache;

    public TweetResource() {
        this.logger=Logger.getLogger(TweetResource.class.getName());

        filesCache = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Page<Blob>>() {
                    @Override
                    public Page<Blob> load(String key) {
                        Storage storage = StorageOptions.getDefaultInstance().getService();
                        return storage.list("epic-collect", Storage.BlobListOption.prefix(key));
                    }
                });
    }

    @GET
    @Path("/test/{event_name}")
    public Response generateRandomFile(@PathParam("event_name") String event_name){
            Random rand = new Random();
            int count=0;
            for(int i=0;i<3000;i++) {
                int x=rand.nextInt(30)-15+1000;
                String tweets="";
                for(int j=0;j<x;j++) {
                    String s="{\"id\":"+count+"}";
                    tweets=tweets.concat(s+"\n");
                    count++;
                }
                String filename = String.format("%s/tweet-%d-%d.json.gz", event_name, (new Date()).getTime(),x);
                Storage storage = StorageOptions.getDefaultInstance().getService();

                BlobId blobId = BlobId.of("epic-collect", filename);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();

                ByteArrayOutputStream obj = new ByteArrayOutputStream();
                try {
                    GZIPOutputStream gzip = new GZIPOutputStream(obj);
                    gzip.write(tweets.getBytes(UTF_8));
                    gzip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                // Store tweets, commit to consumer and clear buffer
                storage.create(blobInfo, obj.toByteArray());
                System.out.println("Done "+i);
            }

        return Response.ok().build();
    }

    @GET
    @Path("/{event_name}/{page_number}/{page_size}")
    public Response getTweets(@PathParam("event_name") String event_name, @PathParam("page_number") IntParam page_number, @PathParam("page_size") IntParam page_size) throws ExecutionException, InterruptedException {
        int searchIndex=(page_number.get()-1)*page_size.get();
        ArrayList<Integer> index = new ArrayList<>();
        Page<Blob> blobs=filesCache.get(event_name);
        int current=0;
        for (Blob blob : blobs.iterateAll()) {
            if(blob.getName().contains(".json.gz")){
                String[] name= blob.getName().split(".json.gz");
                String[] file= name[0].split("-");
                if(current<=searchIndex && current+Integer.parseInt(file[file.length-1])>searchIndex){
                    System.out.println(blob.getName()+"   "+file[file.length-1]);
                    break;
                }
                current=current+Integer.parseInt(file[file.length-1]);

            }
        }

        return Response.ok().build();
    }
}
