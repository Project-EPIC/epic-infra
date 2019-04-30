package edu.colorado.cs.epic.eventsapi.api;

import io.kubernetes.client.models.V1Deployment;

import java.util.List;

public class ExtendedEvent extends Event {

    private List<EventActivity> activity;


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
}
