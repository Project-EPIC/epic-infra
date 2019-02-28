package edu.colorado.cs.epic;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class App 
{
    static Logger log = Logger.getLogger(App.class.getName());

    public static void main( String[] args )
    {
        // Define properties to connect to Kafka
        Properties props = new Properties();
        String kafka_servers = System.getenv().getOrDefault("KAFKA_SERVER", "127.0.0.1:9092");
        props.put("bootstrap.servers", kafka_servers);
        props.put("group.id", System.getenv().getOrDefault("EVENT_NAME","test"));
        props.put("enable.auto.commit", "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Get event keywords
        String[] keywords = System.getenv().getOrDefault("KEYWORDS", "hey").replace(" ","")
                .split(",");


        log.info(String.format("Connecting to Kafka servers: %s", kafka_servers));
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        String topic = "tweets";
        log.info(String.format("Subscribing to topic: %s", topic));
        consumer.subscribe(Arrays.asList(topic));

        final int minBatchSize = 1;
        List<ConsumerRecord<String, String>> buffer = new ArrayList<>();

        log.info("Reading from topic...");
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);

            for (ConsumerRecord<String, String> record : records) {
                for (String keyword : keywords){
                    if (record.value().toLowerCase().contains(keyword.toLowerCase())){
                        buffer.add(record);
                        log.info("Tweet buffered...");
                        break;
                    }
                }
            }

            if (buffer.size() >= minBatchSize) {
                log.info(String.format("Saving %d tweets", buffer.size()));
                // todo: Save and zip
                for (ConsumerRecord<String,String> record : buffer){
                    System.out.println(record.value());
                }
                consumer.commitSync();
                buffer.clear();
            }
        }
    }
}
