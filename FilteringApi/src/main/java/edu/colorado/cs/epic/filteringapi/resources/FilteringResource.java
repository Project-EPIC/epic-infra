package edu.colorado.cs.epic.filteringapi.resources;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Arrays;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@Path("/filtering/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class FilteringResource {
  private final Logger logger;
  private final LoadingCache<String, String> queryTempFilesCache;
  private final BigQuery bigquery;

  public FilteringResource(BigQuery bigqueryClient) {
    this.logger = Logger.getLogger(FilteringResource.class.getName());
    queryTempFilesCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, String>() {
          @Override
          public String load(String key) {
            return "";
          }
        });
    bigquery = bigqueryClient;
  }

  @GET
  @Path("/{eventName}")
  public String getFilteredTweetsByKeywords(@PathParam("eventName") String eventName,
      @QueryParam("keywords") @Size(min = 1) String keywords, // At least one keyword
      @QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
      @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount) throws InterruptedException {

    int pageNumber = page.get();
    int pageSize = pageCount.get();
    String eventTableName = "tweets." + eventName;
    String bqTempFile = "";
    String query = "";
    String[] keywordsArr = keywords.split(",");
    Arrays.sort(keywordsArr); // sort the keywords so we have an expected order

    // Trim keyword strings
    for (int i = 0; i < keywordsArr.length; i++) {
      keywordsArr[i] = keywordsArr[i].trim();
    }

    // when searching for cached results
    // Check if the requested query is found in the cache
    try {
      bqTempFile = queryTempFilesCache.get(eventName + String.join(",", keywordsArr));
    } catch (ExecutionException e) {
      logger.error("Issue accessing Google Cloud", e);
      throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
    }
    if (!bqTempFile.isEmpty()) {
      // Retrieve results from a temporary file cached by BigQuery
      query = String.format("SELECT * FROM `crypto-eon-164220.%s`", bqTempFile);
      return runQuery(query, eventName, keywordsArr, pageNumber, pageSize, false);
    } else {
      // Get results of a new query run by BigQuery
      query = String.format(
          "SELECT id, text, extended_tweet.full_text, created_at,"
              + "user.profile_image_url_https, _File_Name AS filename\n" + "FROM `crypto-eon-164220.%s` WHERE ",
          eventTableName);

      // Build conditional statement for each keyword
      String textConditional = "";
      String extendedTweetConditional = "";
      for (int i = 0; i < keywordsArr.length; i++) {
        textConditional += String.format("text LIKE @keyword%d", i);
        extendedTweetConditional += String.format("extended_tweet.full_text LIKE @keyword%d", i);
        if (i < keywordsArr.length - 1) {
          textConditional += " OR ";
          extendedTweetConditional += " OR ";
        }
      }

      query += textConditional + " OR " + extendedTweetConditional;
      return runQueryquery, eventName, keywordsArr, pageNumber, pageSize, true);
    }
  }

  private String runQuery(String query, String eventName, String[] keywords, Integer pageNumber, Integer pageSize,
      Boolean loadCache) {
    try {
      // Define a BigQuery client
      // BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

      // Build the requested query
      QueryJobConfiguration.Builder queryConfigBuilder = QueryJobConfiguration.newBuilder(query).setUseLegacySql(false)
          .setUseQueryCache(true);

      // Add keywords into query
      for (int i = 0; i < keywords.length; i++) {
        queryConfigBuilder.addNamedParameter("keyword" + i, QueryParameterValue.string("%" + keywords[i] + "%"));
      }
      QueryJobConfiguration queryConfig = queryConfigBuilder.build();

      // Create a job ID to safely retry running the query
      JobId jobId = JobId.of(UUID.randomUUID().toString());
      Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
      // Wait for the query to complete
      queryJob = queryJob.waitFor();

      // Get the results
      QueryJobConfiguration queryJobConfig = queryJob.getConfiguration();

      if (loadCache) {
        // Cache query's temp file if first time requested query, or query not found in
        // cache
        String destDataset = queryJobConfig.getDestinationTable().getDataset();
        String destTable = queryJobConfig.getDestinationTable().getTable();
        queryTempFilesCache.put(eventName + String.join(",", keywords), destDataset + '.' + destTable);
      }

      // Build the final result object with meta data and the requested page of tweets
      StringBuilder tweets = new StringBuilder();

      // Prepare and append tweet list object
      JSONArray tweetArray = new JSONArray();
      TableResult pageToReturn = queryJob.getQueryResults(BigQuery.QueryResultsOption.pageSize(pageSize),
          BigQuery.QueryResultsOption.startIndex((pageNumber - 1) * pageSize));

      // Iterate through the requested page
      int numTweets = 0;
      for (FieldValueList row : pageToReturn.getValues()) {
        JSONObject tweetObject = new JSONObject();
        tweetObject.put("id", row.get("id").getStringValue());
        tweetObject.put("text", row.get("text").getStringValue());
        tweetObject.put("extended_tweet", row.get("full_text").isNull() ? "" : row.get("full_text").getStringValue());
        tweetObject.put("created_at", row.get("created_at").getStringValue());
        tweetObject.put("profile_image", row.get("profile_image_url_https").getStringValue());
        tweetObject.put("filename", row.get("filename").getStringValue());
        tweetArray.add(tweetObject);
        numTweets++;
      }
      tweets.append("{\"tweets\":");
      tweets.append(tweetArray.toJSONString());

      // Prepare and append meta data object
      JSONObject metaObject = new JSONObject();
      metaObject.put("event_name", eventName);
      metaObject.put("keywords", String.join(",", keywords));
      metaObject.put("job_status", queryJob.getStatus().getState().toString());
      metaObject.put("page", pageNumber);
      metaObject.put("count", pageSize);
      metaObject.put("total_count", queryJob.getQueryResults().getTotalRows());
      metaObject.put("num_pages", (int) Math.ceil((double) queryJob.getQueryResults().getTotalRows() / pageSize));
      metaObject.put("tweet_count", numTweets);
      tweets.append(",\"meta\":");
      tweets.append(metaObject.toJSONString());
      tweets.append("}");

      return tweets.toString();

    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }
}