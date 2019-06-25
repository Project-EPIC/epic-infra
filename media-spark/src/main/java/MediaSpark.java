/* MediaSpark.java */
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;


import java.io.File;
import java.util.Date;

public class MediaSpark {
  public static void main(String[] args) {

    // Local log file
    //String logFile = "/Users/isma/Downloads/spark-2.4.0-bin-hadoop2.7/README.md";
    // Create Spark session
    SparkSession spark = SparkSession.builder().appName("Media Spark").getOrCreate();
    //Dataset<String> logData = spark.read().textFile(logFile).cache();

    String eventName = args[0];
    // A JSON dataset is pointed to by path
    // Iterate through the directory to input all the timeline JSON files from an event
    Dataset<Row> timeline = spark.read().json(String.format("gs://epic-collect/%s/*/*/*/*/*", eventName));

    // Creates a temporary view using the DataFrame
    timeline.createOrReplaceTempView("timeline");

    // SQL statements can be run by using the sql methods provided by spark
    // Query the media block from the tweets of the event
    Dataset<Row> namesDF = spark.sql("SELECT user.screen_name AS user, extended_tweet.entities.media AS media FROM timeline");

    // Explode the media array block in order to get all the pictures from the same tweet
    namesDF.select(namesDF.col("user"), explode(namesDF.col("media"))).createOrReplaceTempView("expTimeline");

    // Select the media id, the image link, and the tweet url from the media block
    Dataset<Row> namesDF2 = spark.sql("SELECT user, col.id AS media_id, col.media_url_https  AS image_link, col.expanded_url AS tweet_url FROM expTimeline");

    long count = namesDF2.count();

    // Write the result of the query to our destination JSON file
    namesDF2.coalesce(1).write().mode(SaveMode.Overwrite).json(String.format("gs://epic-analysis-results/spark/media/%s/%d/%d/", eventName, (new Date()).getTime(), count));

    // Stop Spark session
    spark.stop();
  }
}