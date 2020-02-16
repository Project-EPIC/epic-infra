package edu.colorado.cs.epic.filteringapi.resources;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
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
			@QueryParam("allWords") @DefaultValue("") String allWords, // All keywords (AND)
			@QueryParam("phrase") @DefaultValue("") String phrase, // Exact phrase
			@QueryParam("anyWords") @DefaultValue("") String anyWords, // Any keywords (OR)
			@QueryParam("notWords") @DefaultValue("") String notWords, // None of these keywords
			@QueryParam("hashtags") @DefaultValue("") String hashtags, // Hashtags (OR)
			@QueryParam("language") @DefaultValue("") String language, // twitter language code
			@QueryParam("startDate") long startDate, // ms since epoch
			@QueryParam("endDate") long endDate, // ms since epoch
			@QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
			@QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount)
			throws InterruptedException {

		// Modify all strings to be lower case
		allWords = allWords.toLowerCase();
		phrase = phrase.toLowerCase();
		anyWords = anyWords.toLowerCase();
		notWords = notWords.toLowerCase();
		hashtags = hashtags.toLowerCase();

		int pageNumber = page.get();
		int pageSize = pageCount.get();
		String eventTableName = "tweets." + eventName;
		String bqTempFile = "";
		String query = "";
		String twitterUTCTimeFormat = "%a %b %d %H:%M:%S +0000 %Y";

		String[] allWordsArr = allWords.split(",");
		String[] anyWordsArr = anyWords.split(",");
		String[] notWordsArr = notWords.split(",");
		String[] hashtagsArr = hashtags.split(",");

		// sort the keywords so we have an expected order
		Arrays.sort(allWordsArr);
		Arrays.sort(anyWordsArr);
		Arrays.sort(notWordsArr);
		Arrays.sort(hashtagsArr);

		// When searching for cached results
		// check if the requested query is found in the cache
		String paramString = String.format("%s,%s,%s,%s,%s,%s,%d,%d", String.join(",", allWordsArr), phrase,
				String.join(",", anyWordsArr), String.join(",", notWordsArr), String.join(",", hashtagsArr), language,
				startDate, endDate);
		String cacheName = eventName + paramString;
		try {
			bqTempFile = queryTempFilesCache.get(cacheName);
		} catch (ExecutionException e) {
			logger.error("Issue accessing Google Cloud", e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (bqTempFile.isEmpty()) {
			// The cached file is empty so create a new query for BigQuery
			query = String.format("SELECT id_str, text, timestamp_ms, source,"
					+ "user.name, user.screen_name, user.verified, user.created_at, user.description,"
					+ "user.statuses_count, user.favourites_count, user.followers_count, user.friends_count,"
					+ "user.location, user.time_zone, user.url, user.profile_image_url_https\n"
					+ "FROM `crypto-eon-164220.%s` WHERE ", eventTableName);

			Boolean hasCondition = false;

			// Query for tweets that includes all of these words
			if (allWords.length() > 0) {
				query += addQueryCondition(buildTweetTextCondition(allWordsArr, false, true), hasCondition);
				hasCondition = true;
			}

			// Query for tweets that contain this exact phrase
			if (phrase.length() > 0) {
				query += addQueryCondition(buildTweetTextCondition(new String[] { phrase }, false, true), hasCondition);
				hasCondition = true;
			}

			// Query for tweets that include any of these words
			if (anyWords.length() > 0) {
				query += addQueryCondition(buildTweetTextCondition(anyWordsArr, false, false), hasCondition);
				hasCondition = true;
			}

			// Query for tweets that do not contain any of these words
			if (notWords.length() > 0) {
				query += addQueryCondition(buildTweetTextCondition(notWordsArr, true, true), hasCondition);
				hasCondition = true;
			}

			// Query for tweets that include any of these hashtags
			if (hashtags.length() > 0) {
				String hashtagColumn = "entities.hashtags";
				for (int i = 0; i < hashtagsArr.length; i++) {
					// Wrap all the hashtags in single quotes
					hashtagsArr[i] = String.format("'%s'", hashtagsArr[i]);
				}
				String hashtagCondition = String.format(
						"(SELECT COUNT(*) FROM UNNEST(%s) WHERE LOWER(text) IN (%s)) > 0", hashtagColumn,
						String.join(",", hashtagsArr));
				query += addQueryCondition(hashtagCondition, hasCondition);
				hasCondition = true;
			}

			// Query for tweets that were written in a certain language
			if (language.length() > 0) {
				String languageQuery = String.format("lang='%s'", language);
				query += addQueryCondition(languageQuery, hasCondition);
				hasCondition = true;
			}

			// Add date range to the query
			if (startDate != 0 && endDate != 0) {
				query += addQueryCondition(
						String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) BETWEEN %d AND %d)",
								twitterUTCTimeFormat, startDate, endDate),
						hasCondition);
			} else if (startDate != 0) {
				query += addQueryCondition(String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) >= %d)",
						twitterUTCTimeFormat, startDate), hasCondition);
			} else if (endDate != 0) {
				query += addQueryCondition(String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) <= %d)",
						twitterUTCTimeFormat, endDate), hasCondition);
			}
		} else {
			// Retrieve results from a temporary file cached by BigQuery
			query = String.format("SELECT * FROM `crypto-eon-164220.%s`", bqTempFile);
		}

		// Build the requested query
		QueryJobConfiguration.Builder queryConfigBuilder = QueryJobConfiguration.newBuilder(query)
				.setUseLegacySql(false).setUseQueryCache(true);
		QueryJobConfiguration queryConfig = queryConfigBuilder.build();

		return runQuery(queryConfig, paramString, eventName, pageNumber, pageSize, bqTempFile.isEmpty());
	}

	private String buildTweetTextCondition(String[] arr, Boolean isNot, Boolean isAnd) {
		String clause = "(";
		String condition = isAnd ? " AND " : " OR ";
		String notStr = isNot ? " NOT LIKE " : " LIKE ";
		for (int i = 0; i < arr.length; i++) {
			clause += String.format("(LOWER(text) %s '%%%s%%' OR LOWER(extended_tweet.full_text) %s '%%%s%%')", notStr,
					arr[i], notStr, arr[i]);
			if (i < arr.length - 1) {
				clause += condition;
			}
		}
		clause += ")";
		return clause;
	}

	private String addQueryCondition(String condition, boolean hasCondition) {
		return String.format("%s%s", hasCondition ? " AND " : "", condition);
	}

	private String runQuery(QueryJobConfiguration queryConfig, String paramString, String eventName, Integer pageNumber,
			Integer pageSize, Boolean createCache) {
		try {
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

			// Iterate through the requested page and append the tweets to the return object
			int numTweets = 0;
			for (FieldValueList row : pageToReturn.getValues()) {
				JSONObject userObject = new JSONObject();
				userObject.put("name", row.get("name").getStringValue());
				userObject.put("screen_name", row.get("screen_name").getStringValue());
				userObject.put("verified", row.get("verified").getStringValue());
				userObject.put("created_at", row.get("created_at").getStringValue());
				userObject.put("statuses_count", row.get("statuses_count").getStringValue());
				userObject.put("favourites_count", row.get("favourites_count").getStringValue());
				userObject.put("followers_count", row.get("followers_count").getStringValue());
				userObject.put("friends_count", row.get("friends_count").getStringValue());
				userObject.put("profile_image_url_https", row.get("profile_image_url_https").getStringValue());

				// These values can be null so we cannot assume we can get a string value
				userObject.put("description", row.get("description").getValue());
				userObject.put("location", row.get("location").getValue());
				userObject.put("time_zone", row.get("time_zone").getValue());
				userObject.put("url", row.get("url").getValue());

				JSONObject tweetObject = new JSONObject();
				tweetObject.put("id_str", row.get("id_str").getStringValue());
				tweetObject.put("text", row.get("text").getStringValue());
				tweetObject.put("timestamp_ms", row.get("timestamp_ms").getStringValue());
				tweetObject.put("source", row.get("source").getStringValue());
				tweetObject.put("user", userObject);
				tweetArray.add(tweetObject);
				numTweets++;
			}
			tweets.append("{\"tweets\":");
			tweets.append(tweetArray.toJSONString());

			// Prepare and append meta data object to return object
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