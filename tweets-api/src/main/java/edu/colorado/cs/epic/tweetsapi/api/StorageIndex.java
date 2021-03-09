package edu.colorado.cs.epic.tweetsapi.api;

import org.apache.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.json.simple.parser.ParseException;

import edu.colorado.cs.epic.tweetsapi.core.EventDateCount;
import edu.colorado.cs.epic.tweetsapi.core.EventRow;
import edu.colorado.cs.epic.tweetsapi.jdbi3.EventDAO;
import edu.colorado.cs.epic.tweetsapi.jdbi3.EventIndexDAO;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

public class StorageIndex {
    private final Logger logger;
    private final int UNCERTAINTY_SIZE = 10;
    private final int UPLOAD_SIZE = 10000;

    private Bucket tweetStorage;
    private Jdbi jdbi;

    public StorageIndex(Bucket tweetStorage, Jdbi jdbi) {
        this.logger = Logger.getLogger(StorageIndex.class.getName());
        this.tweetStorage = tweetStorage;

        jdbi.registerRowMapper(EventRow.class, (rs, ctx) -> 
            new EventRow(rs.getString("filename"), rs.getLong("timestamp"), rs.getLong("start_index"), rs.getLong("end_index")));
        jdbi.registerRowMapper(EventDateCount.class, (rs, ctx) -> 
            new EventDateCount(rs.getString("time_grouping"), rs.getLong("count")));
        this.jdbi = jdbi;
    }

    public long getEventTweetTotal(String event) {
        return (long) jdbi.withHandle(handle -> {
            EventDAO eventDAO = handle.attach(EventDAO.class);
            EventIndexDAO eventIndexDAO = handle.attach(EventIndexDAO.class);

            int eventId = eventDAO.getEventId(event);
            Long totalCount = eventIndexDAO.getEventTweetTotal(eventId);

            if (totalCount == null) {
                return -1;
            } else {
                return totalCount;
            }
        });
    }

    public synchronized void updateEventIndex(String event) {
        // TODO: Change synchronized to lock based on 'event' name as key
        jdbi.useHandle(handle -> {
            EventDAO eventDAO = handle.attach(EventDAO.class);
            EventIndexDAO eventIndexDAO = handle.attach(EventIndexDAO.class);
    
            eventDAO.insertEvent(event); // This will create a new event entry if it doesn't already exist
            int eventId = eventDAO.getEventId(event);
    
            List<String> uncertainFiles = eventIndexDAO.getLatestFilenames(eventId, UNCERTAINTY_SIZE);
    
            // We will have an uncertainty range for 'ACTIVE' events since files may have been still
            // uploading when we finished updating the index
            if (!uncertainFiles.isEmpty()) {
                Page<Blob> uncertainBlobs = tweetStorage.list(Storage.BlobListOption.prefix(event + "/"),
                    Storage.BlobListOption.fields(Storage.BlobField.NAME),
                    Storage.BlobListOption.startOffset(uncertainFiles.get(0)),
                    Storage.BlobListOption.endOffset(uncertainFiles.get(uncertainFiles.size() - 1)));
    
                boolean differenceFound = false;
                for (Blob blob: uncertainBlobs.iterateAll()) {
                    String blobName = blob.getName();
                    if (blobName.contains(".json.gz") && !uncertainFiles.contains(blob.getName())) {
                        differenceFound = true;
                        break;
                    }
                }
    
                if (differenceFound) {
                    // There's an inconsistency in the index due to files being still uploaded during the last update
                    // we will just regrab all indexes within the uncertainty range if that is the case
                    eventIndexDAO.deleteLatestRows(eventId, UNCERTAINTY_SIZE);
                }
            }
    
            EventRow lastIndex;
            lastIndex = ((lastIndex = eventIndexDAO.getLastRow(eventId)) != null) ? lastIndex : new EventRow();
    
            // Grab any files that have been added since this index file was last updated
            Page<Blob> blobs = tweetStorage.list(Storage.BlobListOption.prefix(event + "/"),
                                            Storage.BlobListOption.fields(Storage.BlobField.NAME),
                                            Storage.BlobListOption.startOffset(lastIndex.getFilename() + " "));
            
            long current = lastIndex.getEndIndex();
            List<EventRow> newFiles = new ArrayList<>();
            boolean newInserts = false;
            int uploadCount = 0;
            for (Blob blob: blobs.iterateAll()) {
                String blobName = blob.getName();
                if (blobName.contains(".json.gz")) {
                    String nameCleaned = blobName.replace(".json.gz", "");
                    String[] fileDetails = nameCleaned.split("-");
    
                    long size = Long.parseLong(fileDetails[fileDetails.length - 1]);
                    long timestamp = Long.parseLong(fileDetails[fileDetails.length - 2]);
    
                    newFiles.add(new EventRow(blobName, timestamp, current, current + size));
    
                    uploadCount++;
                    current += size;
                    newInserts = true;

                    if(uploadCount % UPLOAD_SIZE == 0) {
                        eventIndexDAO.bulkInsertEventIndexEntries(eventId, newFiles);
                        newFiles.clear();
                    }
                }
            }
        
            if (newInserts) {
                eventIndexDAO.bulkInsertEventIndexEntries(eventId, newFiles);
            }
        });
    }

    public String getPaginatedTweets(String event, long startIndex, long endIndex) throws IOException, ParseException {
        List<EventRow> eventRows = jdbi.withHandle(handle -> {
            EventDAO eventDAO = handle.attach(EventDAO.class);
            EventIndexDAO eventIndexDAO = handle.attach(EventIndexDAO.class);
    
            int eventId = eventDAO.getEventId(event);
            return eventIndexDAO.getPaginatedTweets(eventId, startIndex, endIndex);
        });

        StringBuilder tweets = new StringBuilder();
        String separator = "";
        for (EventRow eventRow: eventRows) {
            tweets.append(separator);
            tweets.append(getData(eventRow.getFilename(), startIndex, endIndex, eventRow.getStartIndex(), eventRow.getEndIndex()));
            separator = ",";
        }

        return tweets.toString();
    }

    public String getData(String blobName, long startIndex, long endIndex, long fileStartIndex, long fileEndIndex) 
        throws IOException, ParseException {
        Blob blob = tweetStorage.get(blobName);

        ByteArrayInputStream bais = new ByteArrayInputStream(blob.getContent());
        GZIPInputStream gzis = new GZIPInputStream(bais);
        InputStreamReader reader = new InputStreamReader(gzis);
        LineNumberReader in = new LineNumberReader(reader);

        long start = Math.max(startIndex - fileStartIndex, 0);
        long end = Math.min(endIndex - fileStartIndex, fileEndIndex - fileStartIndex);

        StringBuilder data = new StringBuilder();
        in.lines().skip(start).limit(end-start).forEach(tweet -> {
            data.append(tweet);
            data.append(",");
        });

        // Remove last comma
        if (data.length() > 0) {
            data.setLength(data.length() - 1);
        }

        return data.toString();
    }

    public List<EventDateCount> getEventCounts(String event, String dateFormat) {
        return jdbi.withHandle(handle -> {
            EventDAO eventDAO = handle.attach(EventDAO.class);
            EventIndexDAO eventIndexDAO = handle.attach(EventIndexDAO.class);
    
            int eventId = eventDAO.getEventId(event);
            return eventIndexDAO.getEventCounts(dateFormat, eventId);
        });
    }

    public void updateAllIndexes() {
        jdbi.useHandle(handle -> {
            EventDAO eventDAO = handle.attach(EventDAO.class);
            for (String event: eventDAO.getAllIndexedEvents()) {
                logger.info("Updating index for event: " + event);
                updateEventIndex(event);
            }
        });
    }
}