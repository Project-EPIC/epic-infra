package edu.colorado.cs.epic;

import edu.colorado.cs.epic.TweetMatchStrategy;
import edu.colorado.cs.epic.TweetKeywordStrategy;
import edu.colorado.cs.epic.TweetFollowStrategy;
import edu.colorado.cs.epic.TweetCovid19Strategy;

import com.google.cloud.storage.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TimeoutException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Hello world!
 */
public class App {


    private static Logger log = Logger.getLogger(App.class.getName());

    // Get configuration from environment
    private static final int minBatchSize = Integer.decode(System.getenv().getOrDefault("BATCH_SIZE", "1000"));
    private static final String kafkaTopic = System.getenv().getOrDefault("KAFKA_TOPIC", "tweets");
    private static final String kafkaServers = System.getenv().getOrDefault("KAFKA_SERVER", "127.0.0.1:9092");
    private static final String eventName = System.getenv().getOrDefault("EVENT_NAME", "test");
    private static final String bucketName = System.getenv().getOrDefault("BUCKET_NAME", "epic-collect");
    private static final String matchEnvKey = System.getenv().getOrDefault("MATCH_KEY", "KEYWORDS");
    private static final String[] matchConditions = System.getenv().getOrDefault(matchEnvKey, "hey,me,gerard").split(",");
  
    // Static configuration
    private static final int pollDurationMs = 100;
    private static final String pattern = "yyyy/MM/dd/HH/";

    public static void main(String[] args) {

        // Define properties to connect to Kafka
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaServers);
        props.put("group.id", eventName);
        props.put("enable.auto.commit", "false");
        props.put("max.block.ms", "5000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");


        // Connect to Kafka
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        log.info(String.format("Connecting to Kafka servers: %s", kafkaServers));

        // Subscribe to topic
        consumer.subscribe(Arrays.asList(kafkaTopic));
        log.info(String.format("Subscribing to topic: %s", kafkaTopic));
        log.info(String.format("Group id: %s", eventName));

        // Create buffer and folder variables
        List<ConsumerRecord<String, String>> buffer = new ArrayList<>();
        String folder = new SimpleDateFormat(pattern).format(new Date());

        log.info(String.format("Listening for %s: %s", matchEnvKey, String.join(", ", matchConditions)));

        // Create shutdown hook to save tweets read by current worker and commit results to Kafka
        String finalFolder = folder;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Finishing process...Done");
            saveTweets(consumer, buffer, finalFolder);
        }));
        SignalHandler handler = sig -> {
            log.info("Finishing process...");
            saveTweets(consumer, buffer, finalFolder);
            System.exit(1);
        };
        Signal.handle(new Signal("INT"), handler);
        Signal.handle(new Signal("TERM"), handler);

        // Tweet matching algorithm selection
        TweetMatchStrategy tweetMatch;
        switch (matchEnvKey) {
            case "FOLLOWS":
                tweetMatch = new TweetFollowStrategy();
                break;
            case "COVID19":
                tweetMatch = new TweetCovid19Strategy();
                break;
            case "KEYWORDS":
            default:
                tweetMatch = new TweetKeywordStrategy();
                break;
        }

        while (true) {

            // Check if Kafka connection is up. Kill system otherwise.
            try {
                consumer.listTopics(Duration.ofMillis(1000));
            } catch (TimeoutException e) {
                e.printStackTrace();
                System.exit(1);
            }

            // Poll for records from queue
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollDurationMs));

            // Process all messages
            for (ConsumerRecord<String, String> record : records) {
                for (String match : matchConditions) {
                    boolean isMatch = tweetMatch.isTweetMatch(record.value(), match);
                    if (isMatch) {
                        buffer.add(record);

                        // Check if we need to save tweets (if batchsize has been reached or if we need to dump it because we are changing folder
                        folder = checkFileCreation(consumer, buffer, folder);
                        break;
                    }
                }
            }
            folder = checkFileCreation(consumer, buffer, folder);
        }

    }

    private static String checkFileCreation(KafkaConsumer<String, String> consumer, List<ConsumerRecord<String, String>> buffer, String folder) {
        String currentFolder = new SimpleDateFormat(pattern).format(new Date());
        if (buffer.size() >= minBatchSize || (!folder.equals(currentFolder) && !buffer.isEmpty())) {
            saveTweets(consumer, buffer, folder);
        }
        return currentFolder;
    }

    private static void saveTweets(KafkaConsumer<String, String> consumer, List<ConsumerRecord<String, String>> buffer, String folder) {
        // Calculate filename
        String filename = String.format("%s/%stweet-%d-%d.json.gz", eventName, folder, (new Date()).getTime(), buffer.size());
        log.info(String.format("Saving %d tweets in %s", buffer.size(), filename));

        // Get Google Storage instance
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();

        if (!buffer.isEmpty()) {
            // Convert buffer to new-line delimited json
            String tweets = buffer.stream().map(ConsumerRecord::value).reduce((r1, r2) -> r1 + "\n" + r2).get();

            // GZip tweets
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            try {
                GZIPOutputStream gzip = new GZIPOutputStream(obj);
                gzip.write(tweets.getBytes(UTF_8));
                gzip.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            storage.create(blobInfo, obj.toByteArray());
            buffer.clear();
        }


        // Store tweets, commit to consumer and clear buffer
        consumer.commitSync();
    }
}
