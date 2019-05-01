package edu.colorado.cs.epic.tweetsapi.core;

import edu.colorado.cs.epic.tweetsapi.api.AnnotatedTags;
import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.result.ResultProducer;
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
        postgres.withHandle(handle -> handle.createUpdate("INSERT into tweets (tweet_id, tweet) values (:tweetId,:tweet)")
                .bindBean(annotation)
                .execute()
        );
    }

    private void addTags(TweetAnnotation annotation) {
        postgres.withHandle(handle -> {
            return  handle.createUpdate("INSERT INTO annotation (tweet_id, tag, event_name) VALUES (:tweetId, :tag, :eventName) ON CONFLICT(tweet_id,tag,event_name) do NOTHING")
                    .bind("tag", annotation.getTag())
                    .bind("tweetId", annotation.getTweetId())
                    .bind("eventName", annotation.getEventName())
                    .execute();
        });
    }

    public void deleteAnnotations(AnnotatedTags annotation){
        postgres.withHandle( handle -> {
           return  handle.createUpdate("DELETE from annotation where tag=:tag and tweet_id=:tweetId and event_name=':eventName;")
                   .bind("tag", annotation.getTag())
                   .bind("tweetId", annotation.getTweetId())
                   .bind("eventName", annotation.getEventName())
                   .execute();
        });
    }

    public List<AnnotatedTags> getAnnotations(List<String> tweet_ids, String event_name){
        return postgres.withHandle(handle -> new ArrayList<>(
                handle.select(
                "select tweets.tweet_id as tweetId , tweets.tweet as tweet, annotation.tag as tag, annotation.event_name as eventName, annotation.authuser as authUser from tweets, annotation where tweets.tweet_id in (<tweetIds>) and annotation.event_name= :eventName;")
                .bind("eventName", event_name)
                .bindList("tweetIds", tweet_ids)
                .mapToBean(AnnotatedTags.class)
                .list()
        ));
    }

    public void addAnnotations(TweetAnnotation annotation){
        if(!tweetExists(annotation.getTweetId()))
            addTweet(annotation);
        addTags(annotation);
    }

}
