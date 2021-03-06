package edu.colorado.cs.epic.geotagapi.jdbi3;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import edu.colorado.cs.epic.geotagapi.core.GeoTagIndexRow;

public interface GeoTagIndexDAO {

    @SqlBatch("INSERT INTO geo_tag_index(geo_hash, created_at, username, user_id_str, tweet_id_str, lang, source, in_reply_to_user_id_str, text, image_link, is_retweet, event_id)" +
            "VALUES (:geoTagIndexRow.geoHash, :geoTagIndexRow.createdAt, :geoTagIndexRow.username, :geoTagIndexRow.userIdStr, :geoTagIndexRow.tweetIdStr, :geoTagIndexRow.lang," + 
            ":geoTagIndexRow.source, :geoTagIndexRow.inReplyToUserIdStr, :geoTagIndexRow.text, :geoTagIndexRow.imageLink, :geoTagIndexRow.isRetweet, :eventId) " + 
            "ON CONFLICT(event_id, tweet_id_str) DO NOTHING")
    public void bulkInsertGeoTagIndexEntries(@Bind("eventId") int eventId, @BindBean("geoTagIndexRow") List<GeoTagIndexRow> geoTagIndexRow);

    @SqlQuery("SELECT * FROM geo_tag_index WHERE event_id=:eventId AND geo_hash SIMILAR TO :geoHashPrefixQuery")
    public List<GeoTagIndexRow> getTweetsByGeoHashPrefix(@Bind("eventId") int eventId, @Bind("geoHashPrefixQuery") String geoHashPrefixQuery);
}
    
