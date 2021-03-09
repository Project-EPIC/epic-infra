package edu.colorado.cs.epic.tweetsapi.core;

public class EventRow {
    String filename;
    long timestamp;
    long startIndex;
    long endIndex;

    public EventRow() {
        filename = "";
        timestamp = 0;
        startIndex = 0;
        endIndex = 0;
    }

    public EventRow(String filename, long timestamp, long startIndex, long endIndex) {
        this.filename = filename;
        this.timestamp = timestamp;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setStartindex(long startIndex) {
        this.startIndex = startIndex;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public void setEndIndex(long endIndex) {
        this.endIndex = endIndex;
    }

    public long getEndIndex() {
        return endIndex;
    }
}