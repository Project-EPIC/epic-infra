package edu.colorado.cs.epic.tweetsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;

import javax.validation.constraints.NotNull;

@JsonSnakeCase
public class TranslateRequest {

    @NotNull
    private String source;

    @NotNull
    private String text;

    private String target;

    private static final String DEFAULT_TARGET_LANG = "en";

    public TranslateRequest() {

    }

    @JsonProperty
    public String getSource() {
        return source;
    }

    @JsonProperty
    public void setSource(String source) {
        this.source = source;
    }

    @JsonProperty
    public String getText() {
        return text;
    }

    @JsonProperty
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty
    public String getTarget() {
        if (target==null){
            return DEFAULT_TARGET_LANG;
        }
        return target;
    }

    @JsonProperty
    public void setTarget(String target) {
        this.target = target;
    }
}
