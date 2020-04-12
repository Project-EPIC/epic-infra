package edu.colorado.cs.epic;

import java.util.logging.Logger;

/**
 * Strategy pattern applied match tweets from kafka.
 * Algorithms should vary based on which twitter API endpoint is being consumed
 */
public abstract class TweetMatchStrategy {
    protected static Logger log = Logger.getLogger(TweetFollowStrategy.class.getName());

    public abstract Boolean isTweetMatch(String tweet, String match);
}
