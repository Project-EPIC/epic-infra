package edu.colorado.cs.epic;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Bucket.BucketSourceOption;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobGetOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.lang.*;

public class App {

    public static void main(String[] args) {

        // Get Google Storage instance
        Storage storage = StorageOptions.getDefaultInstance().getService();

        String srcBucketName = "geo-script-testing-data";
        String destBucketName = "geo-script-testing-data2";
        String fileName = "winter_tweet_1555022320703_1000.json";
        String zipfileName = "winter_tweet_1555022320703_1000.json.gz";

        // Get tweeets from a zip tweet json file
        String tweets = readZipTweetJsonFile(storage, srcBucketName, zipfileName);

        String updatedTweets = fixGeoTaggedTweets(tweets);

        // Create a new zip blob of json type to save tweets objects
        uploadZipTweetJsonFile(storage, destBucketName, "winter_tweet_1555022320703_1000_2.json.gz", updatedTweets);

        // // Create a new blob of json type to save tweet objects
        // uploadTweetJsonFile(storage, destBucketName, "winter_tweet_1555022320703_1000_1.json", updatedTweets);
        
        // copyZipTweetJsonFile(storage, 
        //     srcBucketName, 
        //     "winter_tweet_1555022320703_1000.json.gz",
        //     destBucketName, 
        //     "winter_tweet_1555022320703_1000_5.json.gz");

        updateCopyZipTweetJsonFile(storage, 
            srcBucketName, 
            "winter_tweet_1555022320703_1000.json.gz",
            destBucketName, 
            "winter_tweet_1555022320703_1000_5.json.gz");
    }

    private static String readZipTweetJsonFile(Storage storage, String srcFolder, String srcFile) {
        
        // Check if source bucket is found
        Bucket bucket = storage.get(srcFolder, Storage.BucketGetOption.fields(Storage.BucketField.values()));
        if (bucket!=null){      
    
            // Define a buffer for tweet object bytes
            ArrayList<String> buffer = new ArrayList<String>();

            // Define output stream to create a new file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Define input stream to read the original zip file
            Blob blob = storage.get(srcFolder, srcFile);
            ByteArrayInputStream fileInputStream = new ByteArrayInputStream(blob.getContent());
            try {
                GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                // Read the first byte data from the input stream
                int data = gzipInputStream.read();
                // Iterate through all receiving bytes from the input stream
                while(data != -1){                        
                    if ((char)data == '\n') {  
                        // If byte is a new line, 
                        // copy a tweet bytes from the output stream                                          
                        byte byteArray [] = outputStream.toByteArray();
                        // add a new tweet to the buffer
                        String tweet = new String(byteArray);
                        buffer.add(tweet);
                        System.out.println(tweet);
                        System.out.println("`````````````");
                        // Clear the output stream
                        outputStream.reset();
                    }
                    else{
                        // Add byte data to the output stream
                        outputStream.write(data);
                    }
                     // Read byte data from the input stream
                    data = gzipInputStream.read();
                }
                // Add last tweet
                byte byteArray [] = outputStream.toByteArray();
                String tweet = new String(byteArray);
                buffer.add(tweet);
                System.out.println(buffer.size() + " tweet objects have been extracted from " + srcFile + ".");

                // Return a large string containing all tweets found in the buffer,
                // separated by new line return values.  
                String tweets = String.join("\n", buffer);
                gzipInputStream.close();
                buffer.clear();
                return tweets;

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } 
        else {
            System.out.println("The source bucket " + srcFolder + " is not found.");
        }

        return "";
    }

    private static void uploadTweetJsonFile(Storage storage, String folderName, String fileName, String fileContent) {

        if (!fileContent.isEmpty()) {
            // Check if destination bucket is found, create a new one if not.
            Bucket bucket = storage.get(folderName, Storage.BucketGetOption.fields());
            // Bucket bucket = storage.get(folderName, Storage.BucketGetOption.fields(Storage.BucketField.values()));
            if (bucket==null){
                bucket = storage.create(BucketInfo.of(folderName));
                System.out.println("A new destination bucket " + folderName + " has been created.");
            }     
            BlobId blobId = BlobId.of(folderName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
            storage.create(blobInfo, fileContent.getBytes(UTF_8));
            System.out.println("A new blob " + fileName + " has been uploaded.");
        }
    }
                

    private static void updateCopyZipTweetJsonFile(Storage storage, 
        String srcFolder, 
        String srcFile,
        String destFolder, 
        String destFile) {
        
        // Check if source bucket is found
        Bucket srcBucket = storage.get(srcFolder, Storage.BucketGetOption.fields(Storage.BucketField.values()));
        if (srcBucket!=null){                 

            // Define an input stream to read the original zip file
            Blob blob = storage.get(srcFolder, srcFile);
            ByteArrayInputStream inStream = new ByteArrayInputStream(blob.getContent());
            try {
                GZIPInputStream gzipIn = new GZIPInputStream(inStream);

                // Define an output stream to create a new zip file
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                try {
                    GZIPOutputStream gzipOut = new GZIPOutputStream(outStream);
                    int oneByte;
                    ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
                    while ((oneByte = gzipIn.read()) != -1) {
                        if ((char)oneByte != '\n') {
                            tempStream.write(oneByte);                
                        }
                        else{
                            // If byte is a new line, i.e. end of a tweet bytes
                            // create a string tweet object and copy tweet bytes from the temp stream
                            String tweet = new String(tempStream.toByteArray());

                            // Do something with the tweet
                            System.out.println(tweet);
                            System.out.println("`````````````");

                            // Clear the temp stream
                            tempStream.reset();

                            // Add an updated tweet bytes to the output file
                            gzipOut.write(tweet.getBytes(UTF_8));
                            // Add a new line byte
                            gzipOut.write(oneByte);                          
                        }
                        
                    }

                    // If end of file, add the last tweet bytes
                    // create a string tweet object and copy tweet bytes from the temp stream
                    String tweet = new String(tempStream.toByteArray());

                    // Do something with the tweet
                    System.out.println(tweet);
                    System.out.println("`````````````");

                    // Close the temp stream
                    tempStream.close();

                    // Add an updated tweet bytes to the output file
                    gzipOut.write(tweet.getBytes(UTF_8));

                    gzipIn.close();
                    gzipOut.close();

                    // Check if destination bucket is found, create a new one if not.
                    Bucket destBucket = storage.get(destFolder, Storage.BucketGetOption.fields());
                    if (destBucket==null){
                        destBucket = storage.create(BucketInfo.of(destFolder));
                        System.out.println("A new destination bucket " + destFolder + " has been created.");
                    }  
                    // Define and create a new zip blob file
                    BlobId blobId = BlobId.of(destFolder, destFile);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
                    storage.create(blobInfo, outStream.toByteArray());
                    System.out.println("A new blob " + destFile + " has been uploaded.");

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
            System.out.println("The source bucket " + srcFolder + " is not found.");
        }
    }


    private static void copyZipTweetJsonFile(Storage storage, 
        String srcFolder, 
        String srcFile,
        String destFolder, 
        String destFile) {
        
        // Check if source bucket is found
        Bucket srcBucket = storage.get(srcFolder, Storage.BucketGetOption.fields(Storage.BucketField.values()));
        if (srcBucket!=null){      
    
            // Define an input stream to read the original zip file
            Blob blob = storage.get(srcFolder, srcFile);
            ByteArrayInputStream inStream = new ByteArrayInputStream(blob.getContent());
            try {
                GZIPInputStream gzipIn = new GZIPInputStream(inStream);

                // Define an output stream to create a new zip file
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                try {
                    GZIPOutputStream gzipOut = new GZIPOutputStream(outStream);                
                    int oneByte;
                    while ((oneByte = gzipIn.read()) != -1) {
                        gzipOut.write(oneByte);
                    }

                    gzipIn.close();
                    gzipOut.close();

                    // Check if destination bucket is found, create a new one if not.
                    Bucket destBucket = storage.get(destFolder, Storage.BucketGetOption.fields());
                    if (destBucket==null){
                        destBucket = storage.create(BucketInfo.of(destFolder));
                        System.out.println("A new destination bucket " + destFolder + " has been created.");
                    }  
                    // Define and create a new zip blob file
                    BlobId blobId = BlobId.of(destFolder, destFile);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
                    storage.create(blobInfo, outStream.toByteArray());
                    System.out.println("A new blob " + destFile + " has been uploaded.");

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
            System.out.println("The source bucket " + srcFolder + " is not found.");
        }
    }


    private static void uploadZipTweetJsonFile(Storage storage, String folderName, String fileName, String fileContent) {
        
        if (!fileContent.isEmpty()) {
            // Check if destination bucket is found, create a new one if not.
            Bucket bucket = storage.get(folderName, Storage.BucketGetOption.fields());
            // Bucket bucket = storage.get(folderName, Storage.BucketGetOption.fields(Storage.BucketField.values()));
            if (bucket==null){
                bucket = storage.create(BucketInfo.of(folderName));
                System.out.println("A new destination bucket " + folderName + " has been created.");
            }   
            // Define an output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
                gzipOutputStream.write(fileContent.getBytes(UTF_8));
                gzipOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            // Define and create a new zip blob file
            BlobId blobId = BlobId.of(folderName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
            storage.create(blobInfo, outputStream.toByteArray());
            System.out.println("A new blob " + fileName + " has been uploaded.");
        }
    }

    
    private static String fixGeoTaggedTweets(String tweets) {
        // To do next
        return tweets;
    }
        
}
