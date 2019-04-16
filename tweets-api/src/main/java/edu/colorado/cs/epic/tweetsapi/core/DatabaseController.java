package edu.colorado.cs.epic.tweetsapi.core;

import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DatabaseController {

    private Jdbi postgres;

    public DatabaseController(Jdbi postgres) {

        this.postgres = postgres;
    }

    private boolean tweetExists(String tweet_id){
        return postgres.withHandle(handle -> handle.createQuery("SELECT count(*) FROM tweets WHERE tweet_id=:tweet_id")
                .bind("tweet_id", tweet_id)
                .mapTo(Integer.class)
                .findOnly()
        ) > 0;
    }

    private void addTweet(TweetAnnotation annotation){
        postgres.withHandle(handle -> handle.createUpdate("INSERT into tweets (tweet_id, tweet, event_name) values (:tweetId,:tweet,:eventName)")
                .bindBean(annotation)
                .execute()
        );
    }

    private void addTags(TweetAnnotation annotation) {
        postgres.withHandle(handle -> {
            PreparedBatch batch = handle.prepareBatch("INSERT INTO annotation (tweet_id, tag) VALUES (:tweetId, :tag) ON CONFLICT(tweet_id,tag) do NOTHING");
            for (String tag : annotation.getTags()) {
                batch.bind("tag", tag)
                        .bind("tweetId", annotation.getTweetId())
                        .add();
            }
            return batch.execute();
        });
    }


    public void addAnnotations(TweetAnnotation annotation){
        if(!tweetExists(annotation.getTweetId()))
            addTweet(annotation);
        addTags(annotation);
    }

}
