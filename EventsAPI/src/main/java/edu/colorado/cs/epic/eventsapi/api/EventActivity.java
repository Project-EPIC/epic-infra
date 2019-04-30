package edu.colorado.cs.epic.eventsapi.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;

import java.sql.Timestamp;
import java.util.Date;

@JsonSnakeCase
public class EventActivity {

    public EventActivity(){}

    public EventActivity(Type type, String author) {
        this.type = type;
        this.time = new Timestamp(new Date().getTime());
        this.author = author;
    }


    public enum Type {
        START_EVENT, PAUSE_EVENT, CREATE_EVENT
    }

    private Type type;

    private Timestamp time;

    private String author;

    @JsonProperty
    public Type getType() {
        return type;
    }

    @JsonProperty
    public void setType(Type type) {
        this.type = type;
    }

    @JsonProperty
    public Timestamp getTime() {
        return time;
    }

    @JsonProperty
    public void setTime(Timestamp time) {
        this.time = time;
    }

    @JsonProperty
    public String getAuthor() {
        return author;
    }

    @JsonProperty
    public void setAuthor(String author) {
        this.author = author;
    }
}
