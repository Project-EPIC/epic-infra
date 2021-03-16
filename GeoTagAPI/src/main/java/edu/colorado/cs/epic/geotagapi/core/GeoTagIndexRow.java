package edu.colorado.cs.epic.geotagapi.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class GeoTagIndexRow {
    String geoHash;
    String createdAt;
    String username;
    String userIdStr;
    String tweetIdStr;
    String lang;
    String source;
    String inReplyToUserIdStr;
    String text;
    String imageLink;
    boolean isRetweet;

    // This is a workaround for jdbi3 annotations
    public boolean getIsRetweet() {
        return isRetweet;
    }
}