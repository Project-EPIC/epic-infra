package edu.colorado.cs.epic.tweetsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class AnnotatedTags {
    @NotEmpty
    private String tag;
    @NotEmpty
    private String tweetId;
    @NotEmpty
    private String eventName;

    private String authUser;

    public AnnotatedTags(String tag, String tweetId, String eventName) {
        this.tag=tag;
        this.tweetId = tweetId;
        this.eventName = eventName;
    }

    public AnnotatedTags(){}

    @JsonProperty
    public String getTag() {
        return tag;
    }

    @JsonProperty
    public void setTag(String tag) {
        this.tag = tag;
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
