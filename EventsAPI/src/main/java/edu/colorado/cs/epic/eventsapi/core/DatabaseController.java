package edu.colorado.cs.epic.eventsapi.core;

import edu.colorado.cs.epic.eventsapi.api.*;
import org.apache.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.util.*;

/**
 * Created by admin on 19/3/19.
 */
public class DatabaseController {

    private final Logger logger;
    private final Jdbi annotationPostgres;
    private final BigQueryController bqController;
    private Jdbi postgres;

    public DatabaseController(Jdbi postgres, Jdbi annotationPostgres, BigQueryController bqController) {
        logger = Logger.getLogger(DatabaseController.class.getName());
        this.postgres = postgres;
        this.bqController = bqController;
        this.annotationPostgres = annotationPostgres;
    }

    public boolean eventExists(String normalizedName) {
        return postgres.withHandle(handle -> handle.createQuery("SELECT count(*) FROM events WHERE normalized_name=:normalizedName")
                .bind("normalizedName", normalizedName)
                .mapTo(Integer.class)
                .findOnly()
        ) > 0;
    }

    public void insertEvent(Event event) {
        postgres.withHandle(handle -> {
            handle.createUpdate("INSERT INTO events (name, description, normalized_name, status, created_at, author) VALUES (:name,:description,:normalizedName,:status,:createdAt, :author)")
                    .bindBean(event)
                    .execute();
            PreparedBatch batch = handle.prepareBatch("INSERT INTO keywords (event_name, keyword) VALUES (:name, :keyword)");
            for (String keyword : event.getKeywords()) {
                batch.bind("keyword", keyword)
                        .bind("name", event.getNormalizedName())
                        .add();
            }
            handle.createUpdate("INSERT INTO event_activity (type, time, author, event_name) VALUES (:type, :time, :author, :event_name)")
                    .bindBean(new EventActivity(EventActivity.Type.CREATE_EVENT, event.getAuthor()))
                    .bind("event_name", event.getNormalizedName())
                    .execute();
            return batch.execute();
        });
    }


    public List<Event> getEvents() {
        return postgres.withHandle(handle -> new ArrayList<>(handle.createQuery(
                "SELECT e.name e_name, e.author e_author, e.normalized_name e_normalized_name, e.description e_description, e.status e_status, e.created_at e_created_at, k.keyword k_key " +
                        "FROM events e INNER JOIN keywords k ON k.event_name = e.normalized_name " +
                        "ORDER BY e.normalized_name")
                .registerRowMapper(BeanMapper.factory(Event.class, "e"))
                .registerRowMapper(BeanMapper.factory(String.class, "k"))
                .reduceRows(new LinkedHashMap<String, Event>(),
                        (map, rowView) -> {
                            Event event = map.computeIfAbsent(
                                    rowView.getColumn("e_normalized_name", String.class),
                                    id -> rowView.getRow(Event.class)
                            );
                            event.appendKeywords(rowView.getColumn("k_key", String.class));
                            return map;
                        })
                .values()
        ));
    }


    public List<Event> getActiveEvents() {
        return postgres.withHandle(handle -> new ArrayList<>(handle.createQuery(
                "SELECT e.name e_name, e.author e_author, e.normalized_name e_normalized_name, e.description e_description, e.status e_status, e.created_at e_created_at, k.keyword k_key " +
                        "FROM events e INNER JOIN keywords k ON k.event_name = e.normalized_name " +
                        "WHERE e.status = :activeStatus " +
                        "ORDER BY e.normalized_name")
                .bind("activeStatus", Event.Status.ACTIVE.toString())
                .registerRowMapper(BeanMapper.factory(Event.class, "e"))
                .registerRowMapper(BeanMapper.factory(String.class, "k"))
                .reduceRows(new LinkedHashMap<String, Event>(),
                        (map, rowView) -> {
                            Event event = map.computeIfAbsent(
                                    rowView.getColumn("e_normalized_name", String.class),
                                    id -> rowView.getRow(Event.class)
                            );
                            event.appendKeywords(rowView.getColumn("k_key", String.class));
                            return map;
                        })
                .values()
        ));
    }

    public ExtendedEvent getEvent(String normalizedName) {
        ExtendedEvent retEvent = postgres.withHandle(handle -> {
            ExtendedEvent event = handle.createQuery("SELECT * FROM events WHERE normalized_name=:normalizedName ORDER BY normalized_name")
                    .bind("normalizedName", normalizedName)
                    .mapToBean(ExtendedEvent.class)
                    .findOnly();
            event.setKeywords(handle.createQuery("SELECT keyword FROM keywords WHERE event_name=:normalizedName ORDER BY keyword")
                    .bind("normalizedName", event.getNormalizedName())
                    .mapTo(String.class)
                    .list());
            event.setActivity(handle.createQuery("SELECT * FROM event_activity WHERE event_name=:normalizedName ORDER BY time")
                    .bind("normalizedName", event.getNormalizedName())
                    .mapToBean(EventActivity.class)
                    .list());
            return event;

        });
        if (bqController.tableExists(retEvent)) {
            retEvent.setBigQueryTable(retEvent.bigQueryTableName());
        } else {
            retEvent.setBigQueryTable(null);
        }
        return retEvent;
    }

    public void pauseEvent(String normalizedName, String author) {
        postgres.withHandle(handle -> handle.createUpdate("INSERT INTO event_activity (type, time, author, event_name) VALUES (:type, :time, :author, :event_name)")
                .bind("event_name", normalizedName)
                .bindBean(new EventActivity(EventActivity.Type.PAUSE_EVENT, author))
                .execute());
    }

    public void startEvent(String normalizedName, String author) {
        postgres.withHandle(handle -> handle.createUpdate("INSERT INTO event_activity (type, time, author, event_name) VALUES (:type, :time, :author, :event_name)")
                .bind("event_name", normalizedName)
                .bindBean(new EventActivity(EventActivity.Type.START_EVENT, author))
                .execute());
    }

    public void setStatus(String normalizedName, Event.Status status) {
        postgres.withHandle(handle -> handle.createUpdate("UPDATE events set status=:status where normalized_name=:normalizedName")
                .bind("normalizedName", normalizedName)
                .bind("status", status.toString())
                .execute());
    }

    public List<String> getActiveKeywords() {

        return postgres.withHandle(handle -> handle.createQuery("select DISTINCT keyword from keywords,events where event_name=normalized_name and status=:activeStatus")
                .bind("activeStatus", Event.Status.ACTIVE.toString())
                .mapTo(String.class)
                .list());
    }

    public void deleteAnnotation(String eventName, String tweetId, String tag) {
        annotationPostgres.withHandle(handle -> {
            handle.createUpdate("DELETE from annotation where tag=:tag and tweet_id=:tweetId and event_name=':eventName RETURNING *;")
                    .bind("tag", tag)
                    .bind("tweetId", tweetId)
                    .bind("eventName", eventName)
                    .execute();
            try {
                handle.createUpdate("DELETE from tweets where tweet_id=:tweetId;")
                        .bind("tweetId", tweetId)
                        .execute();

            } catch (Exception e) {
                logger.warn("Avoiding tweet deletion");
            }
            return 0;
        });
    }

    public List<TweetAnnotation> getAnnotations(List<String> tweetIds, String eventName) {
        if (tweetIds.isEmpty()) {
            return annotationPostgres.withHandle(handle -> new ArrayList<>(
                    handle.select(
                            "select * from annotation where event_name= :eventName;")
                            .bind("eventName", eventName)
                            .mapToBean(TweetAnnotation.class)
                            .list()
            ));
        }
        return annotationPostgres.withHandle(handle -> new ArrayList<>(
                handle.select(
                        "select * from annotation where tweet_id in (<tweetIds>) and event_name= :eventName;")
                        .bind("eventName", eventName)
                        .bindList("tweetIds", tweetIds)
                        .mapToBean(TweetAnnotation.class)
                        .list()
        ));
    }

    public Optional<TweetAnnotation> getAnnotation(String eventName, String tweetId, String tag) {

        return annotationPostgres.withHandle(handle ->
                handle.select(
                        "select * from annotation where tweet_id = :tweetId and tag = :tag and event_name= :eventName;")
                        .bind("eventName", eventName)
                        .bind("tweetId", tweetId)
                        .bind("tag", tag)
                        .mapToBean(TweetAnnotation.class)
                        .findFirst()
        );
    }

    public List<TweetAnnotation> getAllAnnotations(List<String> tweet_ids) {
        if (tweet_ids.isEmpty()) {
            return annotationPostgres.withHandle(handle -> new ArrayList<>(
                    handle.select(
                            "select * from annotation;")
                            .mapToBean(TweetAnnotation.class)
                            .list()
            ));
        }

        return annotationPostgres.withHandle(handle -> new ArrayList<>(
                handle.select(
                        "select * from annotation where tweet_id in (<tweetIds>);")

                        .bindList("tweetIds", tweet_ids)
                        .mapToBean(TweetAnnotation.class)
                        .list()
        ));
    }

    public void addAnnotation(ExtendedTweetAnnotation annotation) {
        annotationPostgres.withHandle(handle -> {
            handle.createUpdate("INSERT into tweets (tweet_id, tweet) values (:tweetId,:tweet)  ON CONFLICT(tweet_id) do NOTHING")
                    .bindBean(annotation)
                    .execute();
            return handle.createUpdate("INSERT INTO annotation (tweet_id, tag, event_name, authuser) VALUES (:tweetId, :tag, :eventName, :authUser) ON CONFLICT(tweet_id,tag,event_name) do NOTHING")
                    .bindBean(annotation)
                    .execute();
        });
    }

    public boolean annotationExists(ExtendedTweetAnnotation tweetAnnotation){
        return annotationPostgres.withHandle(handle -> handle.createQuery("SELECT count(*) FROM annotation WHERE tweet_id=:tweetId AND tag=:tag AND event_name=:eventName")
                .bindBean(tweetAnnotation)
                .mapTo(Integer.class)
                .findOnly()
        ) > 0;
    }


}
