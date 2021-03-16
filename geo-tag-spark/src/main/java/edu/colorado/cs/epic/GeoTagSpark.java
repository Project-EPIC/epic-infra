package edu.colorado.cs.epic;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.SaveMode;

import scala.collection.mutable.WrappedArray;

import static com.github.davidmoten.geo.GeoHash.encodeHash;
import static org.apache.spark.sql.functions.*;

import java.util.Date;

/**
 * Hello world!
 *
 */
public class GeoTagSpark 
{
	public static void main( String[] args )
    {
        SparkSession spark = SparkSession.builder().appName("GeoTag Spark").getOrCreate();
        
        int HASH_PRECISION = 10;
        String eventName = args[0];

        // Iterate through the directory to input all the timeline JSON files from an event
        Dataset<Row> timeline = spark.read().json(String.format("gs://epic-collect/%s/*/*/*/*/*", eventName));

        // Creates a temporary view using the Dataframe
        timeline.createOrReplaceTempView("timeline");

        Dataset<Row> geoDF = spark.sql("SELECT coordinates.coordinates, place.bounding_box.coordinates AS bounding_box, user.screen_name AS user," + 
                                    " user.id_str AS user_id_str, created_at, id_str AS tweet_id_str, lang, source, in_reply_to_user_id_str, text, " + 
                                    " extended_tweet.entities.media AS media, retweeted_status FROM timeline WHERE coordinates IS NOT NULL OR place IS NOT NULL"); 

        UserDefinedFunction coordsToGeoHash = udf((WrappedArray<Double> exactCoords, WrappedArray<WrappedArray<WrappedArray<Double>>> boundingBox) -> {
            if (exactCoords != null) {
                return encodeHash(exactCoords.apply(1), exactCoords.apply(0), HASH_PRECISION);
            } else {
                WrappedArray<Double> lowerLeft = boundingBox.apply(0).apply(0);
                WrappedArray<Double> upperRight = boundingBox.apply(0).apply(2);

                Double centerLon = (lowerLeft.apply(0) + upperRight.apply(0)) / 2;
                Double centerLat = (lowerLeft.apply(1) + upperRight.apply(1)) / 2;
                return encodeHash(centerLat, centerLon, HASH_PRECISION);
            }
        }, DataTypes.StringType);

        geoDF.select(coordsToGeoHash.apply(geoDF.col("coordinates"), geoDF.col("bounding_box")).alias("geo_hash"), geoDF.col("user"), geoDF.col("user_id_str"), 
                    geoDF.col("created_at"), geoDF.col("tweet_id_str"), geoDF.col("lang"), geoDF.col("source"), geoDF.col("in_reply_to_user_id_str"), 
                    geoDF.col("text"),  geoDF.col("retweeted_status"), explode(geoDF.col("media"))).createOrReplaceTempView("expTimeline");

        Dataset<Row> geoDF2 = spark.sql("SELECT user, user_id_str, geo_hash, created_at, tweet_id_str, lang, source, " + 
                        "in_reply_to_user_id_str, text, retweeted_status, col.media_url_https AS image_link FROM expTimeline");

        // Write the result of the query to our destination JSON file
        geoDF2
            .coalesce(1)
            .write()
            .option("compression", "gzip")
            .mode(SaveMode.Overwrite)
            .json(String.format("gs://epic-analysis-results/spark/geotag/%s/%d/", eventName, (new Date()).getTime()));

        spark.stop();
    }
}
