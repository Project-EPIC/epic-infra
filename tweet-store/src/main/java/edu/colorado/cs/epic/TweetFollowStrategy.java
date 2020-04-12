package edu.colorado.cs.epic;

import edu.colorado.cs.epic.TweetMatchStrategy;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

/**
 * Match tweets on userIds
 */
public class TweetFollowStrategy extends TweetMatchStrategy {
    public Boolean isTweetMatch(String tweet, String match) {
        try {
            // Check that the tweet object's user id matches
            JSONParser parser = new JSONParser();
            JSONObject tweetObj = (JSONObject)parser.parse(tweet);
            
            String userId = (String)((JSONObject)tweetObj.get("user")).get("id_str");
            
            return userId.equals(match);
        }
        catch (ParseException e) {
            log.info("Unable to parse tweet");
        }

        return false;
    }
}
