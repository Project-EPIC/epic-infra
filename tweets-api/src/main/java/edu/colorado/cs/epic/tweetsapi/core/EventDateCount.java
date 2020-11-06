package edu.colorado.cs.epic.tweetsapi.core;

public class EventDateCount {
    String dateStr;
    int count;

    public EventDateCount(String dateStr, int count) {
        this.dateStr = dateStr.replace(" ", "T");
        this.count = count;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
