package edu.colorado.cs.epic;

import edu.colorado.cs.epic.TweetMatchStrategy;

/**
 * Match all tweets
 */
public class TweetCovid19Strategy extends TweetMatchStrategy {
    public Boolean isTweetMatch(String tweet, String match) {
        return true;
    }
}
