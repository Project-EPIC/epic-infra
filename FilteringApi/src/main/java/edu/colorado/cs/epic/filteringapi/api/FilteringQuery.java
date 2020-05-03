package edu.colorado.cs.epic.filteringapi.api;

import edu.colorado.cs.epic.filteringapi.api.Predicate;

import javax.validation.constraints.NotNull;
import javax.validation.Valid;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilteringQuery {
    String twitterUTCTimeFormat = "%a %b %d %H:%M:%S +0000 %Y";

    @Valid
    Predicate predicates[];

    long startDate;
    long endDate;
    String language;
    String hashtags;

    public FilteringQuery() {
    }

    public FilteringQuery(Predicate predicates[], long startDate, long endDate, String language, String hashtags) {
        this.predicates = predicates;
        this.startDate = startDate;
        this.endDate = endDate;
        this.language = language != null ? language : "";
        this.hashtags = hashtags != null ? hashtags : "";
    }

    @JsonProperty
    public void setPredicates(Predicate predicates[]) {
        this.predicates = predicates;
    }

    @JsonProperty
    public Predicate[] getPredicates() {
        return this.predicates;
    }

    @JsonProperty
    public void setStartDate(@NotNull long startDate) {
        this.startDate = startDate;
    }

    @JsonProperty
    public long getStartDate() {
        return this.startDate;
    }

    @JsonProperty
    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    @JsonProperty
    public long getEndDate() {
        return this.endDate;
    }

    @JsonProperty
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonProperty
    public String getLanguage() {
        return this.language;
    }

    @JsonProperty
    public void setHashtags(String hashtags) {
        String[] tmp = hashtags.split(",");
        Arrays.sort(tmp);

        this.hashtags = String.join(",", tmp);
    }

    @JsonProperty
    public String getHashtags() {
        return this.hashtags;
    }

    public String paramString() {
        String paramString = "(";
        if (predicates != null) {
            for (int i = 0; i < predicates.length; i++) {
                Predicate predicate = predicates[i];
                if (i > 0) {
                    paramString += predicate.isOr ? ")OR(" : ")AND(";
                }
                paramString += predicate.paramString();
            }
        }
        paramString += ")";
        return String.format("%s,%s,%s,%d,%d", paramString, hashtags, language, startDate, endDate);
    }

    public String getQueryString(String eventTableName) {
        if (eventTableName == null || eventTableName == "") {
            return ""; // TODO: Throw exception here
        }

        String query = String.format("SELECT id_str, text, timestamp_ms, source,"
                + "user.name, user.screen_name, user.verified, user.created_at, user.description,"
                + "user.statuses_count, user.favourites_count, user.followers_count, user.friends_count,"
                + "user.location, user.time_zone, user.url, user.profile_image_url_https\n"
                + "FROM `crypto-eon-164220.%s` WHERE ", eventTableName);

        boolean hasCondition = true;

        // Add date range to the query
        if (startDate != 0 && endDate != 0) {
            query += String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) BETWEEN %d AND %d)",
                    twitterUTCTimeFormat, startDate, endDate);
        } else if (startDate != 0) {
            query += String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) >= %d)", twitterUTCTimeFormat,
                    startDate);
        } else if (endDate != 0) {
            query += String.format("(UNIX_MILLIS(PARSE_TIMESTAMP('%s',created_at)) <= %d)", twitterUTCTimeFormat,
                    endDate);
        } else {
            hasCondition = false; // shorthand way so I don't need to set to true in above if statements
        }

        // Query only for tweets written in a specific language
        if (language != null && language.length() > 0) {
            String languageQuery = String.format("lang='%s'", language);
            query += addQueryCondition(languageQuery, hasCondition);
            hasCondition = true;
        }

        // Query for tweets that include any of these hashtags
        if (hashtags != null && hashtags.length() > 0) {
            String[] hashtagsArr = hashtags.split(",");
            String hashtagColumn = "entities.hashtags";
            for (int i = 0; i < hashtagsArr.length; i++) {
                // Wrap all the hashtags in single quotes
                hashtagsArr[i] = String.format("'%s'", hashtagsArr[i]);
            }
            String hashtagCondition = String.format("(SELECT COUNT(*) FROM UNNEST(%s) WHERE LOWER(text) IN (%s)) > 0",
                    hashtagColumn, String.join(",", hashtagsArr));
            query += addQueryCondition(hashtagCondition, hasCondition);
            hasCondition = true;
        }

        // Add text conditions to query
        if (predicates != null && predicates.length > 0) {
            String queryPredicate = buildQueryPredicate();
            query += addQueryCondition(queryPredicate, hasCondition);
            hasCondition = true;
        }

        return query;
    }

    private String addQueryCondition(String condition, boolean hasCondition) {
        return String.format("%s%s", hasCondition ? " AND " : "", condition);
    }

    private String buildQueryPredicate() {
        String queryPredicate = "(";

        for (int i = 0; i < predicates.length; i++) {
            Predicate predicate = predicates[i];

            if (i > 0) {
                queryPredicate += predicate.isOr ? ") OR (" : ") AND (";
            }

            queryPredicate += predicate.buildQueryPredicate();
        }

        queryPredicate += ")";
        return queryPredicate;
    }

}