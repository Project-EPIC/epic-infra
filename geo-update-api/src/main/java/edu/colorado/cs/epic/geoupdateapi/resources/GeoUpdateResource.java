package edu.colorado.cs.epic.geoupdateapi.resources;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
// import com.google.cloud.storage.Bucket.BucketSourceOption;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
// import com.google.cloud.storage.Storage.BlobSourceOption;
// import com.google.cloud.storage.Storage.BlobGetOption;
import com.google.cloud.storage.StorageOptions;
// import com.google.cloud.storage.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.UnsupportedEncodingException;

import java.util.*;
import java.util.concurrent.*;
// import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import java.time.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Arrays;

import javax.annotation.security.RolesAllowed;
// import javax.validation.constraints.Max;
// import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
// import javax.ws.rs.core.Response;
// import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
// import org.json.simple.JSONArray;
// import org.json.simple.JSONObject;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.OrderedJSONObject;

// import edu.colorado.cs.epic.geoupdate.*;


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

      // App App = new App();
      int totalTweetCount = asynEventUpdate(eventName, srcbucket, destbucket);

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


  private static Integer asynEventUpdate(String eventName, String srcBucketName, String destBucketName) {

    // Get Google Storage instance
    Storage storage = StorageOptions.getDefaultInstance().getService();
    // Define the source bucket
    Bucket bucket = storage.get(srcBucketName, Storage.BucketGetOption.fields(Storage.BucketField.values()));

    ExecutorService executor = Executors.newCachedThreadPool();
    Set<Callable<Integer>> callables = new HashSet<Callable<Integer>>();

    // Iterate through all blobs found in the event folder in the source bucket
    BlobListOption blobListOption = Storage.BlobListOption.prefix(eventName);
    for (Blob currentBlob : bucket.list(blobListOption).iterateAll()) {
        if (currentBlob.getName().endsWith("json.gz")) {
            callables.add(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return updateTweetData(storage, srcBucketName, currentBlob.getName(), destBucketName, currentBlob.getName());
                }
            });
        }

    }

    try {
        int c = 0;
        List<Future<Integer>> futures = executor.invokeAll(callables);
        // for (Future<String> future : futures){
        //     try {
        //         c = c +1;
        //         System.out.println(c + ": " + future.get());
        //     } catch (Exception e) {
        //         throw new IllegalStateException(e);
        //     }
        // }
        int sum = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).mapToInt(Integer::intValue).sum();

        System.out.println("\nTotal tweet count: " + sum);
        return sum;

    } catch (InterruptedException e) {
        e.printStackTrace();
    } finally {
        // shut down the executor manually
        executor.shutdown();
    }
    return 0;
}
  
  private static Integer updateTweetData(Storage storage, String srcFolder, String srcFile, String destFolder, String destFile) {

    int count = 0;
    // System.out.println(srcFile);
    // Check if source bucket is found
    Bucket srcBucket = storage.get(srcFolder, Storage.BucketGetOption.fields(Storage.BucketField.values()));
    if (srcBucket != null) {
    
        Blob blob = storage.get(srcFolder, srcFile);
        if (blob != null) {
            // Define an input stream to read an original zip file
            ByteArrayInputStream inStream = new ByteArrayInputStream(blob.getContent());
            try {
                GZIPInputStream gzipIn = new GZIPInputStream(inStream);

                // Define an output stream to create a new zip file
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                try {
                    GZIPOutputStream gzipOut = new GZIPOutputStream(outStream);
                    // Define a temp stream to read tweet objects
                    ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
                    int oneByte;
                    while ((oneByte = gzipIn.read()) != -1) {

                        if ((char) oneByte != '\n') { //oneByte == 10
                            tempStream.write(oneByte);
                        } else {

                            // If byte is a new line, i.e. end of a tweet bytes,
                            // Increment tweet count
                            count = count + 1;
                            System.out.printf("\rTweet count: %-8d file: %-100s", count, srcFile);
                            // printf("'%-5d'", 10);
                            // System.out.printf("\r%s: %d", srcFile, count);
                            // Create a string tweet object and copy tweet bytes from the temp stream
                            String tweet = new String(tempStream.toByteArray(), "UTF-8");

                            // Update tweet object
                            String updatedTweet = fixGeoTaggedTweet(tweet);
                            // System.out.println(updatedTweet);
                            // System.out.println("`````````````");
                            
                            // Clear the temp stream
                            tempStream.reset();

                            // Add an updated tweet bytes to the output file
                            gzipOut.write(updatedTweet.getBytes(UTF_8));
                            // Add a new line byte
                            gzipOut.write(oneByte);
                        }
                    }

                    // If end of file, add the last tweet bytes,
                    // Increment tweet count
                    count = count + 1;
                    // System.out.printf("\rTweet count: %d\n", count);
                    System.out.printf("\rTweet count: %-8d file: %-100s", count, srcFile);

                    // Create a string tweet object and copy tweet bytes from the temp stream
                    String tweet = new String(tempStream.toByteArray(), "UTF-8");
                    
                    // Update tweet object
                    String updatedTweet = fixGeoTaggedTweet(tweet);
                    // System.out.println(updatedTweet);
                    // System.out.println("`````````````");

                    // Close the temp stream
                    tempStream.close();

                    // Add an updated tweet bytes to the output file
                    gzipOut.write(updatedTweet.getBytes(UTF_8));

                    gzipIn.close();
                    gzipOut.close();

                    // Check if destination bucket is found, create a new one if not.
                    Bucket destBucket = storage.get(destFolder, Storage.BucketGetOption.fields());
                    if (destBucket == null) {
                        destBucket = storage.create(BucketInfo.of(destFolder));
                        // System.out.println("INFO: A new destination bucket " + destFolder + " has been created.");
                    }
                    // Define and create a new zip blob file
                    BlobId blobId = BlobId.of(destFolder, destFile);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
                    storage.create(blobInfo, outStream.toByteArray());
                    // System.out.println("INFO: A new blob " + destFile + " has been uploaded.");
                    // System.out.printf("\rINFO: %s has been uploaded.", destFile);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }              
        }
        else {
            System.out.println("ERROR: The source file " + srcFile + " is not found.");
        }           
    } else {
        System.out.println("ERROR: The source bucket " + srcFolder + " is not found.");
    }
    return count;
  }

  private static String fixGeoTaggedTweet(String tweet) {
    if (!tweet.isEmpty()){
        try {
            // OrderedJSONObject is an extension of the basic JSONObject, provided by Apache Wink,
            // that keeps the order while parsing the String.
            // This class allows control of the serialization order of attributes. 
            // The order in which items are put into the instance controls the order in which they are serialized out. 
            // https://wink.apache.org/documentation/1.1.2/api/org/apache/wink/json4j/OrderedJSONObject.html
          
                    
            // ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(myString)
            // String value = new String(tweet, "UTF-8");
            // new String(tweet, "UTF-8");
            OrderedJSONObject tweetJSON = new OrderedJSONObject(tweet);
            // tweetJSON = tweet.toJ
            OrderedJSONObject updatedTweetJSON = parseNestedJson(tweetJSON);
            // System.out.println(tweet);
            // System.out.println("........"); 
            // System.out.println(tweetJSON);
            // System.out.println("______________"); 

            return updatedTweetJSON.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    return tweet;
  }

  private static OrderedJSONObject parseNestedJson(OrderedJSONObject obj) throws JSONException {
    // A recursive function that reads a tweet object and its nested tweet objects, 
    // stringifies any occurances of place/bounding_box attribute, then
    // returns an updated tweet object.

    if (obj instanceof OrderedJSONObject) {

        Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = obj.get(key);
            
            if (key.equals("place") && value != null){
                OrderedJSONObject placeJson = (OrderedJSONObject) value;
                if (placeJson.get("bounding_box") != null){          
                    placeJson.put("bounding_box", placeJson.get("bounding_box").toString());
                    obj.put(key, placeJson);   
                }           
            }
            else if (Arrays.asList("retweeted_status", "quoted_status").contains(key) &&
                    value instanceof OrderedJSONObject){
                obj.put(key, parseNestedJson((OrderedJSONObject) value));                  
            }
        }
    }
    return obj;
  }

  public static String tweetGeoUpdate(String srcBuffer) throws IOException {

    if (!srcBuffer.isEmpty()) {

      int count = 0;

      // Define an input stream for the received string buffer
      ByteArrayInputStream inStream = new ByteArrayInputStream(srcBuffer.getBytes(UTF_8));
      // Define an output stream to create a new zip file
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      try {
        // GZIPOutputStream gzipOut = new GZIPOutputStream(outStream);
        // Define a temp stream to read tweet objects
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        int oneByte;
        while ((oneByte = inStream.read()) != -1) {

          if ((char) oneByte != '\n') { // oneByte == 10
            tempStream.write(oneByte);
          } else {

            // If byte is a new line, i.e. end of a tweet bytes,
            // Increment tweet count
            count = count + 1;
            System.out.printf("\rTweet count: %-8d file: %-100s", count);

            // Create a string tweet object and copy tweet bytes from the temp stream
            String tweet = new String(tempStream.toByteArray(), "UTF-8");

            // Update tweet object
            String updatedTweet = fixGeoTaggedTweet(tweet);
            // System.out.println(updatedTweet);
            // System.out.println("`````````````");

            // Clear the temp stream
            tempStream.reset();

            // Add an updated tweet bytes to the output stream
            outStream.write(updatedTweet.getBytes(UTF_8));
            // Add a new line byte
            outStream.write(oneByte);
          }
        }

        // If end of file, add the last tweet bytes,
        // Increment tweet count
        count = count + 1;
        // System.out.printf("\rTweet count: %d\n", count);
        System.out.printf("\rTweet count: %-8d file: %-100s", count);

        // Create a string tweet object and copy tweet bytes from the temp stream
        String tweet = new String(tempStream.toByteArray(), "UTF-8");

        // Update tweet object
        String updatedTweet = fixGeoTaggedTweet(tweet);
        // System.out.println(updatedTweet);
        // System.out.println("`````````````");

        // Close the temp stream
        tempStream.close();

        // Add an updated tweet bytes to the output file
        outStream.write(updatedTweet.getBytes(UTF_8));

        inStream.close();
        outStream.close();

        return outStream.toString();

      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }    
    return "";
  }

}