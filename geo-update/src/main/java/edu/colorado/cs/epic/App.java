package edu.colorado.cs.epic;

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
// import java.io.UnsupportedEncodingException;

import java.util.*;
import java.util.concurrent.*;
// import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import java.time.*;

// import org.json.simple.JSONObject;
// import org.json.simple.parser.JSONParser;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.OrderedJSONObject;

public class App {

    public static void main(String[] args) {

        // String srcBucketName = "epic-historic-tweets";
        // String destBucketName = "epic-historic-tweets-new";
        // String srcfileName = "2012 Hurricane Sandy/tweets_0.json.gz";
        // String destfileName = "2012 Hurricane Sandy/tweets_0_new.json.gz";

        System.out.println("\nStart processing ...");
        Instant start = Instant.now();

        // Define a source bucket name
        String srcBucketName = "epic-collect";
        String destBucketName = "epic-collect-new";
        String eventName= "winter";
        int totalTweetCount = asynEventUpdate(eventName, srcBucketName, destBucketName);
        // eventUpdate(eventName, srcBucketName, destBucketName);
        // updateTweetData(storage, srcBucketName, srcfileName, destBucketName, destfileName);
        System.out.println("\nTotal tweet count: " + totalTweetCount);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis(); //in millis
        System.out.println(String.format("Processing time: %d millis", timeElapsed));

        // application/json
        // application/x-gzip

        // Storage storage = StorageOptions.getDefaultInstance().getService();
        // readZipTweetJsonFile(storage, "geo-script-testing-data",
        // "tweet_problem.json");

        // updateTweetData(storage, "epic-collect",
        // "winter/2019/04/14/02/tweet-1555210800000-768.json.gz",
        // "geo-script-testing-data", "tweet-1555210800000-768-4.json.gz");

    }

    private static String readZipTweetJsonFile(Storage storage, String srcFolder, String srcFile) {
        // Check if source bucket is found
        Bucket bucket = storage.get(srcFolder, Storage.BucketGetOption.fields(Storage.BucketField.values()));
        if (bucket != null) {

            // Define a buffer for tweet object bytes
            ArrayList<String> buffer = new ArrayList<String>();

            // Define output stream to create a new file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Define input stream to read the original zip file
            Blob blob = storage.get(srcFolder, srcFile);
            ByteArrayInputStream fileInputStream = new ByteArrayInputStream(blob.getContent());
            try {
                // GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                // Read the first byte data from the input stream
                int data = fileInputStream.read();
                // Iterate through all receiving bytes from the input stream
                while (data != -1) {
                    if ((char) data == '\n') {
                        // If byte is a new line,
                        // copy a tweet bytes from the output stream
                        byte byteArray[] = outputStream.toByteArray();
                        // add a new tweet to the buffer
                        String tweet = new String(byteArray);
                        String updatedTweet = fixGeoTaggedTweet(tweet);
                        buffer.add(updatedTweet);
                        System.out.println(updatedTweet);
                        System.out.println("`````````````");
                        // Clear the output stream
                        outputStream.reset();
                    } else {
                        // Add byte data to the output stream
                        outputStream.write(data);
                    }
                    // Read byte data from the input stream
                    data = fileInputStream.read();
                }
                // Add last tweet
                byte byteArray[] = outputStream.toByteArray();
                String tweet = new String(byteArray);
                // String updatedTweet = fixGeoTaggedTweet(tweet);
                buffer.add(tweet);
                System.out.println(tweet);
                System.out.println("`````````````");
                System.out.println(buffer.size() + " tweet objects have been extracted from " + srcFile + ".");

                // Return a large string containing all tweets found in the buffer,
                // separated by new line return values.
                String tweets = String.join("\n", buffer);
                fileInputStream.close();
                buffer.clear();
                return tweets;

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.out.println("The source bucket " + srcFolder + " is not found.");
        }

        return "";
    }

    public static Integer asynEventUpdate(String eventName, String srcBucketName, String destBucketName) {

        int sum = 0; 
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
            List<Future<Integer>> futures = executor.invokeAll(callables);
            sum = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).mapToInt(Integer::intValue).sum();

            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // shut down the executor manually
            executor.shutdown();
        }
        return sum;
    }

    public static void eventUpdate(String eventName, String srcBucketName, String destBucketName){

        // Get Google Storage instance 
        Storage storage = StorageOptions.getDefaultInstance().getService();
        // Define the source bucket
        Bucket bucket = storage.get(srcBucketName, Storage.BucketGetOption.fields(Storage.BucketField.values()));
       
        // List the blobs in a particular bucket
        System.out.println("Available files:");
        int c = 0;
        BlobListOption blobListOption = Storage.BlobListOption.prefix(eventName);
        for (Blob currentBlob: bucket.list(blobListOption).iterateAll()) {
            if (currentBlob.getName().endsWith("json.gz") // && currentBlob.getContentType().equals("application/json")
                ){
                c = c +1;
                System.out.println(c + ": " + currentBlob.getName());
                // updateTweetData(storage, srcBucketName, currentBlob.getName(), destBucketName, currentBlob.getName());
            }

        }
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

// http://www.avajava.com/tutorials/lessons/how-do-i-compress-and-uncompress-a-gzip-file.html