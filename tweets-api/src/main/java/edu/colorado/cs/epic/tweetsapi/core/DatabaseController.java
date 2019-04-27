package edu.colorado.cs.epic.tweetsapi.core;

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
            PreparedBatch batch = handle.prepareBatch("INSERT INTO annotation (tweet_id, tag, event_name) VALUES (:tweetId, :tag, :eventName) ON CONFLICT(tweet_id,tag,event_name) do NOTHING");
            for (String tag : annotation.getTags()) {
                batch.bind("tag", tag)
                        .bind("tweetId", annotation.getTweetId())
                        .bind("eventName", annotation.getEventName())
                        .add();
            }
            return batch.execute();
        });
    }

    public List<TweetAnnotation> getAnnotations(List<String> tweet_ids, String event_name){
//        List<String> x=postgres.withHandle(handle -> {
//            return handle.select("select annotation.tag as tags from tweets, annotation where tweets.tweet_id in (<tweetIds>) and annotation.event_name= :eventName;")
//                .bind("eventName", event_name)
//                .bindList("tweetIds", tweet_ids)
//                    .mapTo(String.class)
//                    .list();
//
//
//        });
//        for(String x1:x)
//        System.out.println(x1);
//                return new ArrayList<TweetAnnotation>();
        return postgres.withHandle(handle -> new ArrayList<>(
                handle.select(
                "select tweets.tweet_id as tweetId , tweets.tweet as tweet, annotation.tag as tags, annotation.event_name as eventName, annotation.authuser as authUser from tweets, annotation where tweets.tweet_id in (<tweetIds>) and annotation.event_name= :eventName;")
                .bind("eventName", event_name)
                .bindList("tweetIds", tweet_ids)
                .registerRowMapper(BeanMapper.factory(TweetAnnotation.class))
                        .reduceRows(new LinkedHashMap<String, TweetAnnotation>(),
                                (map, rowView) ->{
                                    TweetAnnotation t=map.get(rowView.getColumn(1, String.class));
                                    if(t==null){
                                        t= new TweetAnnotation();
                                        t.setEventName(rowView.getColumn("eventName", String.class));
                                        t.setTweet(rowView.getColumn("tweet", String.class));
                                        t.setTweetId(rowView.getColumn("tweetId", String.class));
                                        t.setAuthUser(rowView.getColumn("authUser", String.class));
                                        t.appendTags(rowView.getColumn("tags", String.class));
                                        map.put(rowView.getColumn("tweetId", String.class), t);
                                    }else{
                                        t.appendTags(rowView.getColumn("tags", String.class));
                                    }

                                    return map;
                                })
                .values()
        ));
    }

    public void addAnnotations(TweetAnnotation annotation){
        if(!tweetExists(annotation.getTweetId()))
            addTweet(annotation);
        addTags(annotation);
    }

}
