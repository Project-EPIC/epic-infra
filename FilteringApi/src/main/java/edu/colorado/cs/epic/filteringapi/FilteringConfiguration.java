package edu.colorado.cs.epic.filteringapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class FilteringConfiguration extends Configuration {
    @NotNull
    private Boolean production;

    public FilteringConfiguration() {
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
