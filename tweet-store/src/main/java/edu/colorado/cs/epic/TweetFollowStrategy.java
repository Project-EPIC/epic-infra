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
            
            String tweetCreatorUserId = (String)((JSONObject)tweetObj.get("user")).get("id_str");
            String inReplyToUserId = (String)tweetObj.get("in_reply_to_user_id_str");

            String retweetByUserId = null;
            if (tweetObj.containsKey("retweeted_status") && tweetObj.get("retweeted_status") != null) {
                JSONObject retweetObj = (JSONObject)tweetObj.get("retweeted_status");
                retweetByUserId = (String)((JSONObject)retweetObj.get("user")).get("id_str");
            }
            
            return match.equals(tweetCreatorUserId) || match.equals(inReplyToUserId) || match.equals(retweetByUserId);
        }
        catch (ParseException e) {
            log.info("Unable to parse tweet");
        }

        return false;
    }
}
