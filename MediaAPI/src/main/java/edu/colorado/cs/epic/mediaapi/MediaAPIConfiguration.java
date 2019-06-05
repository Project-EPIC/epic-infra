package edu.colorado.cs.epic.mediaapi;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.*;
import javax.validation.constraints.*;

public class MediaAPIConfiguration extends Configuration {
    @NotNull
    private Boolean production;


    public MediaAPIConfiguration() {

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
