package edu.colorado.cs.epic.tweetsapi.jdbi3;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface EventDAO {

    @SqlUpdate("INSERT INTO events(name) VALUES (:eventName) ON CONFLICT DO NOTHING")
    public void insertEvent(@Bind("eventName") String eventName);

    @SqlQuery("SELECT id FROM events WHERE name=:eventName")
    public int getEventId(@Bind("eventName") String eventName);

    @SqlQuery("SELECT name FROM events")
    public List<String> getAllIndexedEvents();   

}
