package edu.colorado.cs.epic.tweetsapi.jdbi3;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import edu.colorado.cs.epic.tweetsapi.core.EventDateCount;
import edu.colorado.cs.epic.tweetsapi.core.EventRow;

public interface EventIndexDAO {

    @SqlBatch("INSERT INTO event_index(filename, timestamp, start_index, end_index, event_id) VALUES (:eventRow.filename, :eventRow.timestamp, :eventRow.startIndex, :eventRow.endIndex, :eventId)")
    public void bulkInsertEventIndexEntries(@Bind("eventId") int eventId, @BindBean("eventRow") List<EventRow> eventRows);

    @SqlUpdate("DELETE FROM event_index WHERE id=any(array(SELECT id FROM event_index WHERE event_id=:event_id ORDER BY id DESC LIMIT :limit))")
    public void deleteLatestRows(@Bind("eventId") int eventId, @Bind("limit") int uncertainty);

    @SqlQuery("SELECT end_index FROM event_index WHERE event_id=:eventId ORDER BY end_index DESC LIMIT 1")
    public Long getEventTweetTotal(@Bind("eventId") int eventId);

    @SqlQuery("SELECT filename FROM event_index WHERE event_id=:eventId ORDER BY id LIMIT :uncertainty")
    public List<String> getLatestFilenames(@Bind("eventId") int eventId, @Bind("uncertainty") int uncertainty);

    @SqlQuery("SELECT * FROM event_index WHERE event_id=:eventId ORDER BY id DESC LIMIT 1")
    public EventRow getLastRow(@Bind("eventId") int eventId);

    @SqlQuery("SELECT * FROM event_index WHERE event_id=:eventId and ((start_index <= :start AND end_index > :start) OR (start_index <= :end AND end_index > :end))")
    public List<EventRow> getPaginatedTweets(@Bind("eventId") int eventId, @Bind("start") long start, @Bind("end") long end);

    @SqlQuery("SELECT to_char(to_timestamp(timestamp/1000) AT TIME ZONE 'GMT', :dateFormat) as time_grouping, sum(end_index::bigint-start_index::bigint) as count FROM event_index WHERE event_id=:eventId GROUP BY time_grouping ORDER BY time_grouping ASC")
    public List<EventDateCount> getEventCounts(@Bind("dateFormat") String dateFormat, @Bind("eventId") int eventId);

}
