package edu.colorado.cs.epic.tweetsapi.core;

public class EventDateCount {
    String dateStr;
    long count;

    public EventDateCount(String dateStr, long count) {
        this.dateStr = dateStr.replace(" ", "T");
        this.count = count;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
