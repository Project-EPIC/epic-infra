package edu.colorado.cs.epic.auth;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.*;
import javax.validation.constraints.*;

public class AuthAPIConfiguration extends Configuration {

    @NotEmpty
    private String projectId;

    // Enables authentication
    @NotNull
    private Boolean production;

    @JsonProperty
    public String getProjectId() {
        return projectId;
    }

    @JsonProperty
    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
