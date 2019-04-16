package edu.colorado.cs.epic.tweetsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class TweetAnnotation {
    @NotEmpty
    private List<String> tags = new ArrayList<>();
    @NotEmpty
    private String tweet;
    @NotEmpty
    private String tweetId;
    @NotEmpty
    private String eventName;

    public TweetAnnotation(List<String> tags, String tweet, String tweetId, String eventName) {
        this.tags = tags;
        this.tweet = tweet;
        this.tweetId = tweetId;
        this.eventName = eventName;
    }

    public TweetAnnotation(){}
    @JsonProperty
    public List<String> getTags() {
        return tags;
    }

    @JsonProperty
    public void setTags(List<String> tags) {
        this.tags = tags;
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
}

