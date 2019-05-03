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

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
  
  public FilteringResource() {
    this.logger = Logger.getLogger(FilteringResource.class.getName());
    queryTempFilesCache = CacheBuilder.newBuilder()
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build(new CacheLoader<String, String>() {
          @Override
          public String load(String key) {
            return "";
          }
      });
  }

  @GET
  @Path("/{eventName}/{keyword}") 
  public String getFilteredTweets(@PathParam("eventName") String eventName,
                                  @PathParam("keyword") String keyword,
                                  @QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
                                  @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount)                             
                                  throws InterruptedException {
                                
    int pageNumber = page.get();
    int pageSize = pageCount.get();  
    String eventTableName = "tweets."+eventName;
    String bqTempFile = "";
    String query = "";

    // Check if the requested query is found in the cache                             
    try { 
      bqTempFile = queryTempFilesCache.get(eventName+keyword);
    } catch (ExecutionException e) {
      logger.error("Issue accessing Google Cloud", e);
      throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
    }
    
    if (!bqTempFile.isEmpty()) {
      // Rertieve results from a temporary file cached by BigQuery 
      query = String.format("SELECT * FROM `crypto-eon-164220.%s`", bqTempFile);
      return runQuery(query, eventName, keyword, pageNumber, pageSize, false);
    }
    else
      // Get results of a new query run by BigQuery 
      query = String.format("SELECT text, extended_tweet.full_text, created_at,"
      + "user.profile_image_url_https, _File_Name AS filename\n"
      + "FROM `crypto-eon-164220.%s`\n"
      + "WHERE text LIKE @keyword OR "
      + "extended_tweet.full_text IS NOT NULL AND "
      + "extended_tweet.full_text LIKE @keyword\n", eventTableName);
      return runQuery(query, eventName, keyword, pageNumber, pageSize, true);
    
  }

  private String runQuery(String query, String eventName, String keyword, Integer pageNumber, Integer pageSize, Boolean loadCache) {
    try {
      // Define a BigQuery client
      BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

      // Build the requsted query
      String formattedkeyword = "%#" + keyword + "%";
      QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query)    
          .setUseLegacySql(false)
          .setUseQueryCache(true)
          .addNamedParameter("keyword", QueryParameterValue.string(formattedkeyword))        
          .build();

      // Create a job ID to safely retry running the query
      JobId jobId = JobId.of(UUID.randomUUID().toString());
      Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

      // Wait for the query to complete
      queryJob = queryJob.waitFor();

      // Get the results
      QueryJobConfiguration queryJobConfig = queryJob.getConfiguration();
      
      if (loadCache) {
        // Cache query's temp file if first time requested query, or query not found in cache
        String destDataset = queryJobConfig.getDestinationTable().getDataset();
        String destTable = queryJobConfig.getDestinationTable().getTable();
        queryTempFilesCache.put(eventName+keyword, destDataset+'.'+destTable);
      }

      // Build the final result object with meta data and the requested page of tweets
      StringBuilder tweets = new StringBuilder();

      // Prepare and append meta data object
      JSONObject metaObject = new JSONObject();     
      metaObject.put("event_name", eventName);
      metaObject.put("keyword", keyword);       
      metaObject.put("job_status", queryJob.getStatus().getState().toString());
      metaObject.put("page", pageNumber);
      metaObject.put("count", pageSize);
      metaObject.put("total_count", queryJob.getQueryResults().getTotalRows());
      metaObject.put("num_pages", (int) Math.ceil((double) queryJob.getQueryResults().getTotalRows() / pageSize));       
      tweets.append("{\"meta\":");
      tweets.append(metaObject.toJSONString());     

      // Prepare and append tweet list object
      JSONArray tweetArray = new JSONArray();
      TableResult pageToReturn = queryJob.getQueryResults(
        BigQuery.QueryResultsOption.pageSize(pageSize),
        BigQuery.QueryResultsOption.startIndex((pageNumber - 1) * pageSize)
      );      
      // Iterate through the requested page
      for (FieldValueList row : pageToReturn.getValues()) { 
        JSONObject tweetObject = new JSONObject();         
        tweetObject.put("text", row.get("text").getStringValue()); 
        tweetObject.put("extended_tweet", row.get("full_text").isNull()?"":row.get("full_text").getStringValue()); 
        tweetObject.put("created_at", row.get("created_at").getStringValue()); 
        tweetObject.put("profile_image", row.get("profile_image_url_https").getStringValue()); 
        tweetObject.put("filename", row.get("filename").getStringValue()); 
        tweetArray.add(tweetObject);
      } 
      tweets.append(",\"tweets\":");
      tweets.append(tweetArray.toJSONString());
      tweets.append("}");
      return tweets.toString();

    } catch (Exception e) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }

}
