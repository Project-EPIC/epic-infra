package edu.colorado.cs.epic.tweetsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;


public class TweetAnnotation {
    @NotEmpty
    private String tag;
    @NotEmpty
    private String tweet;
    @NotEmpty
    private String tweetId;
    @NotEmpty
    private String eventName;

    private String authUser;

    public TweetAnnotation(String tag, String tweet, String tweetId, String eventName) {
        this.tag = tag;
        this.tweet = tweet;
        this.tweetId = tweetId;
        this.eventName = eventName;
    }

    public TweetAnnotation(){}
    @JsonProperty
    public String getTag() { return tag; }

    @JsonProperty
    public void setTag(String tag) {
        this.tag = tag;
    }

    @JsonProperty
    public String getTweet() {
        return tweet;
    }

    @JsonProperty
    public void setTweet(String tweet) {
        this.tweet = tweet;
    }

    @JsonProperty
    public String getTweetId() {
        return tweetId;
    }

    @JsonProperty
    public void setTweetId(String tweetId) {
        this.tweetId = tweetId;
    }

    @JsonProperty
    public String getEventName() { return eventName; }

    @JsonProperty
    public void setEventName(String eventName) { this.eventName = eventName; }

    @JsonProperty
    public String getAuthUser() { return authUser; }

    @JsonProperty
    public void setAuthUser(String authUser) { this.authUser = authUser; }
}

