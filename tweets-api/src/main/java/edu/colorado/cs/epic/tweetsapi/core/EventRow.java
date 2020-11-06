package edu.colorado.cs.epic.tweetsapi.core;

public class EventRow {
    String filename;
    long timestamp;
    int startIndex;
    int endIndex;

    public EventRow() {
        filename = "";
        timestamp = 0;
        startIndex = 0;
        endIndex = 0;
    }

    public EventRow(String filename, long timestamp, int startIndex, int endIndex) {
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
    public void setStartindex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}