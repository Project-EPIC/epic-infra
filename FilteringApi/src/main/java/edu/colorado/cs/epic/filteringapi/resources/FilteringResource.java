package edu.colorado.cs.epic.filteringapi.resources;

// import com.google.api.gax.paging.Page;
// import com.google.cloud.storage.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
// import javax.ws.rs.core.MediaType;
// import javax.ws.rs.core.Response;

// import com.google.common.collect.Iterables;
import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
// import org.json.simple.JSONObject;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.TableResult;

import java.util.Optional;
import java.util.UUID;

@Path("/filtering/")
// @Produces(MediaType.APPLICATION_JSON)
// @Consumes(MediaType.APPLICATION_JSON)
public class FilteringResource {
  private final Logger logger;

  public FilteringResource() {
    this.logger = Logger.getLogger(FilteringResource.class.getName());
  }

  @GET
  @Path("/{eventName}/{keyword}")
  public String getFilteredTweets(@PathParam("eventName") String eventName,
                                  @PathParam("keyword") String keyword,
                                  @QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
                                  @QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount) 
                                  throws InterruptedException {
                        
    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
    String formatedkeyword = "%#" + keyword + "%";
    QueryJobConfiguration queryConfig =
      QueryJobConfiguration.newBuilder(
        "SELECT "
          + "text, extended_tweet.full_text, created_at,"
          + "user.profile_image_url_https, _File_Name AS filename\n"
          + "FROM `crypto-eon-164220.tweets.winter`\n"
          + "WHERE text LIKE @keyword OR "
          + "extended_tweet.full_text IS NOT NULL AND "
          + "extended_tweet.full_text LIKE @keyword\n"
          // + "ORDER BY created_at DESC"
          + "LIMIT 4")
        .setUseLegacySql(false)
        .addNamedParameter("keyword", QueryParameterValue.string(formatedkeyword))
        .build();

    // // Create a job ID so that we can safely retry.
    // JobId jobId = JobId.of(UUID.randomUUID().toString());
    // Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

    // // Wait for the query to complete.
    // queryJob = queryJob.waitFor();

    // // Check for errors
    // if (queryJob == null) {
    //   throw new RuntimeException("Job no longer exists");
    // } else if (queryJob.getStatus().getError() != null) {
    //   // You can also look at queryJob.getStatus().getExecutionErrors() for all
    //   // errors, not just the latest one.
    //   throw new RuntimeException(queryJob.getStatus().getError().toString());
    // }

    // Get the results.
    // QueryResponse response = bigquery.getQueryResults(jobId);
    // TableResult result = queryJob.getQueryResults();

    // Print all pages of the results.
    StringBuilder tweets = new StringBuilder();
    tweets.append(String.format("{\"event\":\"%s\",", eventName));
    tweets.append(String.format("\"keyword\":\"%s\",", keyword));
    tweets.append(String.format("\"page\":\"%s\",", page));
    tweets.append(String.format("\"count\":\"%s\",", pageCount));
    tweets.append(String.format("\"tweets\":["));
    // for (FieldValueList row : result.iterateAll()) {
    for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
      String text = row.get("text").getStringValue();
      // String extended_tweet = row.get("full_text").getStringValue();
      // String extended_tweet = Optional.ofNullable(row.get("full_text").getStringValue()).orElse("");
      // String extended_tweet = (row.get("full_text").getStringValue() == null) ? "" : row.get("full_text").getStringValue();
      String created_at = row.get("created_at").getStringValue();
      String profile_image = row.get("profile_image_url_https").getStringValue();
      String filename = row.get("filename").getStringValue();
      
      tweets.append(String.format("{\"text\":\"%s\",", text));
      // tweets.append(String.format("\"extended_tweet\":\"%s\",", extended_tweet));
      tweets.append(String.format("\"created_at\":\"%s\",", created_at));
      tweets.append(String.format("\"profile_image\":\"%s\",", profile_image));
      tweets.append(String.format("\"filename\":\"%s\"},", filename));
    } 
    // remove last comma
    tweets.setLength(tweets.length() - 1);
    tweets.append("]}");
    return tweets.toString();
  }
}
