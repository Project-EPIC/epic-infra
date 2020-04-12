package edu.colorado.cs.epic;

import edu.colorado.cs.epic.TweetMatchStrategy;

import java.util.*;

/**
 * Match tweets on keywords
 */
public class TweetKeywordStrategy extends TweetMatchStrategy {
    public Boolean isTweetMatch(String tweet, String match) {
        String lowerTweet = tweet.toLowerCase();

        // Checking that tweet contains ALL words in the match string 
        String[] terms = match.trim().split(" ");

        // Check that the tweet contains all terms
        return Arrays.stream(terms).allMatch(term -> lowerTweet.contains(term.toLowerCase()));
    }
}
