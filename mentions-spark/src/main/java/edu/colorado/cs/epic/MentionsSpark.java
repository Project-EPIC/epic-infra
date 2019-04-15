package edu.colorado.cs.epic;/* edu.colorado.cs.epic.MentionsSpark.java */
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;


import java.io.File;

public class MentionsSpark {
  public static void main(String[] args) {

    // Local log file
    //String logFile = "/Users/isma/Downloads/spark-2.4.0-bin-hadoop2.7/README.md";
    // Create Spark session
    SparkSession spark = SparkSession.builder().appName("edu.colorado.cs.epic.MentionsSpark").getOrCreate();
    //Dataset<String> logData = spark.read().textFile(logFile).cache();

    // A JSON dataset is pointed to by path
    // Iterate through the directory to input all the timeline JSON files from an event
    Dataset<Row> timeline = spark.read().json(args[0]);
    // Print all the JSON files
    //timeline.printSchema();

    // Creates a temporary view using the DataFrame
    timeline.createOrReplaceTempView("timeline");

    // SQL statements can be run by using the sql methods provided by spark
    // Query all the user mentions, retweet count and favourite count from the users mentioned from the JSON files
    Dataset<Row> namesDF = spark.sql("SELECT entities.user_mentions, retweet_count, favorite_count FROM timeline WHERE retweeted_status IS NULL");

    // Explode the set of mentioned users inside "user_mentions" and create a temporal view with the retweets, likes and this
    namesDF.select(namesDF.col("retweet_count"), namesDF.col("favorite_count"), explode(namesDF.col("user_mentions"))).createOrReplaceTempView("expTimeline");

    //Dataset<Row> namesDF3 = spark.sql("SELECT * FROM expTimeline");
    //namesDF3.printSchema();

    // Query:
    // - Mentioned user ids
    // - List of screen names of that user
    // - times they have been mentioned in total in descendent order
    // - Total sum of retweets when this user is mentioned
    // - Total sum of likes when this user is mentioned
    // Group by user ids
    Dataset<Row> namesDF2 = spark.sql("SELECT col.id AS user_id, COLLECT_LIST(DISTINCT col.screen_name) AS user, COUNT(col.id) AS times_mentioned, SUM(retweet_count) AS total_retweets, SUM(favorite_count) AS total_likes FROM expTimeline GROUP BY col.id ORDER BY times_mentioned DESC");

    //namesDF2.printSchema();

    // Write the result of the query to our destination JSON file
    namesDF2.coalesce(1).write().mode(SaveMode.Overwrite).json(args[1]);

    /** USEFUL CODE:

     Seq<String> fieldNamesSeq = JavaConverters.asScalaIteratorConverter(Arrays.asList(fieldNames).iterator()).asScala().toSeq();
     String json = row.getValuesMap(fieldNamesSeq).toString();

     userMentions.foreach(mention -> {
     System.out.println(mention);
     });

     **/

    // Stop Spark session
    spark.stop();
  }
}