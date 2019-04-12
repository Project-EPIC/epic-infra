package edu.colorado.cs.epic.tweetsapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;


public class TweetsConfiguration extends Configuration {

    @NotNull
    private Boolean production;


    public TweetsConfiguration() {

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
