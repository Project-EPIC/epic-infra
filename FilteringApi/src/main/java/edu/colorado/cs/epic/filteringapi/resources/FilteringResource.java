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

	public FilteringResource() {
		this.logger = Logger.getLogger(FilteringResource.class.getName());
		queryTempFilesCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {
					@Override
					public String load(String key) {
						return "";
					}
				});
	}

	@GET
	@Path("/{eventName}")
	public String getFilteredTweetsByKeywords(@PathParam("eventName") String eventName,
			@QueryParam("allWords") @DefaultValue("") String allWords, // All keywords (AND)
			@QueryParam("phrase") @DefaultValue("") String phrase, // Exact phrase
			@QueryParam("anyWords") @DefaultValue("") String anyWords, // Any keywords (OR)
			@QueryParam("notWords") @DefaultValue("") String notWords, // None of these keywords
			@QueryParam("startDate") String startDate, @QueryParam("endDate") String endDate,
			@QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
			@QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount)
			throws InterruptedException {

		// Modify all strings to be lower case
		allWords = allWords.toLowerCase();
		phrase = phrase.toLowerCase();
		anyWords = anyWords.toLowerCase();
		notWords = notWords.toLowerCase();

		int pageNumber = page.get();
		int pageSize = pageCount.get();
		String eventTableName = "tweets." + eventName;
		String bqTempFile = "";
		String query = "";

		String[] allWordsArr = allWords.split(",");
		String[] anyWordsArr = anyWords.split(",");
		String[] notWordsArr = notWords.split(",");

		// sort the keywords so we have an expected order
		Arrays.sort(allWordsArr);
		Arrays.sort(anyWordsArr);
		Arrays.sort(notWordsArr);

		// when searching for cached results
		// Check if the requested query is found in the cache
		String paramString = String.join(",", allWordsArr) + phrase + String.join(",", anyWordsArr)
				+ String.join(",", notWordsArr);
		String cacheName = eventName + paramString;
		try {
			bqTempFile = queryTempFilesCache.get(cacheName);
		} catch (ExecutionException e) {
			logger.error("Issue accessing Google Cloud", e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (bqTempFile.isEmpty()) {
			// The cached file is empty so create a new query for BigQuery
			query = String.format("SELECT id, text, extended_tweet.full_text, created_at,"
					+ "user.profile_image_url_https, _File_Name AS filename\n" + "FROM `crypto-eon-164220.%s` WHERE ",
					eventTableName);

			Boolean hasClause = false;

			if (allWords.length() > 0) {
				// Add conditional clause in query to include only tweets that includes all of
				// these words
				query += buildConditionalClause(allWordsArr, false, true);
				hasClause = true;
			}

			if (phrase.length() > 0) {
				// Add conditional clause in the query to include only tweets that contain this
				// exact phrase
				if (hasClause) {
					query += " AND ";
				}
				hasClause = true;
				query += buildConditionalClause(new String[]{phrase}, false, true);
			}

			if (anyWords.length() > 0) {
				// Add conditional clause in the query to include tweets that includes any of
				// these words
				if (hasClause) {
					query += " AND ";
				}
				hasClause = true;
				query += buildConditionalClause(anyWordsArr, false, false);
			}

			if (notWords.length() > 0) {
				// Add conditonal clause in the query to include tweets that do not contain all
				// of these words
				if (hasClause) {
					query += " AND ";
				}
				query += buildConditionalClause(notWordsArr, true, true);
			}
		} else {
			// Retrieve results from a temporary file cached by BigQuery
			query = String.format("SELECT * FROM `crypto-eon-164220.%s`", bqTempFile);
		}

		// Build the requested query
		QueryJobConfiguration.Builder queryConfigBuilder = QueryJobConfiguration.newBuilder(query)
				.setUseLegacySql(false).setUseQueryCache(true);
		QueryJobConfiguration queryConfig = queryConfigBuilder.build();

		System.out.println(query);
		return "";
		// return runQuery(queryConfig, paramString, eventName, pageNumber, pageSize, bqTempFile.isEmpty());
	}

	private String buildConditionalClause(String[] arr, Boolean isNot, Boolean isAnd) {
		String clause = "(";
		String condition = isAnd ? " AND " : " OR ";
		String notStr = isNot ? " NOT LIKE " : " LIKE ";
		for (int i = 0; i < arr.length; i++) {
			clause += String.format("(LOWER(text) %s '%%%s%%' OR LOWER(extended_tweet.full_text) %s '%%%s%%')", notStr, arr[i], notStr, arr[i]);
			if (i < arr.length - 1) {
				clause += condition;
			}
		}
		clause += ")";
		return clause;
	}

	private String runQuery(QueryJobConfiguration queryConfig, String paramString, String eventName, Integer pageNumber,
			Integer pageSize, Boolean createCache) {
		try {
			// Define a BigQuery client
			BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

			// Create a job ID to safely retry running the query
			JobId jobId = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

			// Wait for the query to complete
			queryJob = queryJob.waitFor();

			// Get the results
			QueryJobConfiguration queryJobConfig = queryJob.getConfiguration();

			if (createCache) {
				// Cache query's temp file if first time requested query, or query not found in
				String destDataset = queryJobConfig.getDestinationTable().getDataset();
				String destTable = queryJobConfig.getDestinationTable().getTable();
				queryTempFilesCache.put(eventName + paramString, destDataset + '.' + destTable);
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
				tweetObject.put("extended_tweet",
						row.get("full_text").isNull() ? "" : row.get("full_text").getStringValue());
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
			metaObject.put("params", paramString);
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