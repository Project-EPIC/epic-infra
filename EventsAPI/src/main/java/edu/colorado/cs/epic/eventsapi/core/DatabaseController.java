package edu.colorado.cs.epic.eventsapi.core;

import edu.colorado.cs.epic.eventsapi.api.Event;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.util.ArrayList;
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
            handle.createUpdate("INSERT INTO events (name, description, normalized_name, status, created_at) VALUES (:name,:description,:normalizedName,:status,:createdAt)")
                    .bindBean(event)
                    .execute();
            PreparedBatch batch = handle.prepareBatch("INSERT INTO keywords (event_name, keyword) VALUES (:name, :keyword)");
            for (String keyword : event.getKeywords()) {
                batch.bind("keyword", keyword)
                        .bind("name", event.getNormalizedName())
                        .add();
            }
            return batch.execute();
        });
    }


    public List<Event> getEvents() {
        return postgres.withHandle(handle -> new ArrayList<>(handle.createQuery(
                "SELECT e.name e_name, e.normalized_name e_normalized_name, e.description e_description, e.status e_status, e.created_at e_created_at, k.keyword k_key " +
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
                "SELECT e.name e_name, e.normalized_name e_normalized_name, e.description e_description, e.status e_status, e.created_at e_created_at, k.keyword k_key " +
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

    public Event getEvent(String normalizedName) {

        return postgres.withHandle(handle -> {
            Event event = handle.createQuery("SELECT * FROM events WHERE normalized_name=:normalizedName ORDER BY normalized_name")
                    .bind("normalizedName", normalizedName)
                    .mapToBean(Event.class)
                    .findOnly();
            event.setKeywords(handle.createQuery("SELECT keyword FROM keywords WHERE event_name=:normalizedName ORDER BY keyword")
                    .bind("normalizedName", event.getNormalizedName())
                    .mapTo(String.class)
                    .list());
            return event;

        });
    }

    public void setStatus(String normalizedName, Event.Status status) {
        postgres.withHandle(handle -> handle.createUpdate("UPDATE events set status=:staus where normalized_name=:normalizedName")
                .bind("normalizedName", normalizedName)
                .bind("staus", status.toString())
                .execute());
    }

    public List<String> getActiveKeywords() {

        return postgres.withHandle(handle -> handle.createQuery("select DISTINCT keyword from keywords,events where event_name=normalized_name and status=:activeStatus")
                .bind("activeStatus", Event.Status.ACTIVE.toString())
                .mapTo(String.class)
                .list());
    }

}
