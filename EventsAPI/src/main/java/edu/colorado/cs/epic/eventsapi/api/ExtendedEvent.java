package edu.colorado.cs.epic.eventsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import io.kubernetes.client.models.V1Deployment;

import java.net.URI;
import java.util.List;

@JsonSnakeCase
public class ExtendedEvent extends Event {

    private List<EventActivity> activity;
    private URI bigQueryTableURL;


    public ExtendedEvent() {
    }


    public ExtendedEvent(V1Deployment deployment) {
        super(deployment);
    }

    public ExtendedEvent(String name, List<String> keywords, String description) {
        super(name, keywords, description);
    }

    public List<EventActivity> getActivity() {
        return activity;
    }

    public void setActivity(List<EventActivity> activity) {
        this.activity = activity;
    }

    @JsonProperty
    public URI getBigQueryTableURL() {
        return bigQueryTableURL;
    }

    @JsonProperty
    public void setBigQueryTableURL(URI bigQueryTableURL) {
        this.bigQueryTableURL = bigQueryTableURL;
    }

}
