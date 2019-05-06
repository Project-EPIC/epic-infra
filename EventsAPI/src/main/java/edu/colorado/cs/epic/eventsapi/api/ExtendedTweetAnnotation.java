package edu.colorado.cs.epic.eventsapi.api;

import org.hibernate.validator.constraints.NotEmpty;

public class ExtendedTweetAnnotation extends  TweetAnnotation {
    @NotEmpty
    private String tweet;

    public ExtendedTweetAnnotation(String tag, String tweetId, String eventName, String tweet) {
        super(tag, tweetId, eventName);
        this.tweet = tweet;
    }

    public ExtendedTweetAnnotation() {
    }

    public String getTweet() {
        return tweet;
    }

    public void setTweet(String tweet) {
        this.tweet = tweet;
    }
}
