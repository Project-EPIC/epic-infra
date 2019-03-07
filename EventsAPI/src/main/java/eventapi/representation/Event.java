package eventapi.representation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Event {
    private long id;
    private String name;
    private List<String> keywords;
    private String description;

    public Event() {
        // Jackson deserialization
    }

    public Event(String name, List<String> keywords, String description) {
        this.name = name;
        this.keywords=keywords;
        this.description=description;

    }
    @JsonProperty
    public String getName() {
        return name;
    }
    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }
    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }
    @JsonProperty
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    @JsonProperty
    public String getDescription() {
        return description;
    }
    @JsonProperty
    public void setDescription(String description) {
        this.description = description;
    }
}