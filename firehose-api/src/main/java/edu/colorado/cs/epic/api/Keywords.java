package edu.colorado.cs.epic.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;

import java.util.List;

/**
 * Created by admin on 7/3/19.
 */
@JsonSnakeCase
public class Keywords {

    private List<String> keywords;

    public Keywords() {
        // Jackson deserialization
    }

    public Keywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }
}
