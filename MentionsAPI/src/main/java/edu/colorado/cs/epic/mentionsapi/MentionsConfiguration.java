package edu.colorado.cs.epic.mentionsapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class MentionsConfiguration extends Configuration {
    @NotNull
    private Boolean production;


    public MentionsConfiguration() {

    }

    @JsonProperty
    public Boolean getProduction() {
        return production;
    }

    @JsonProperty
    public void setProduction(Boolean production) {
        this.production = production;
    }
}
