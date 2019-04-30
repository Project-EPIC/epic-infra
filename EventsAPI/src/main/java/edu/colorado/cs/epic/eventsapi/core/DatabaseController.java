package edu.colorado.cs.epic.eventsapi.core;

import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.api.EventActivity;
import edu.colorado.cs.epic.eventsapi.api.ExtendedEvent;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by admin on 19/3/19.
 */
public class DatabaseController {

    private Jdbi postgres;

    public DatabaseController(Jdbi postgres) {

        this.postgres = postgres;
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

        return postgres.withHandle(handle -> {
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
    }

    public void pauseEvent(String normalizedName, String author) {
        postgres.withHandle(handle -> handle.createUpdate("INSERT INTO event_activity (type, time, author, event_name) VALUES (:type, :time, :author, :event_name)")
                .bind("event_name", normalizedName)
                .bindBean(new EventActivity(EventActivity.Type.PAUSE_EVENT,author))
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

}
