package eventapi.representation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String name;
    private List<String> keywords=new ArrayList<String>();
    private String description;
    private  String normalizedName;
    public Event() {
        // Jackson deserialization
    }

    public Event(String name, List<String> keywords, String description) {
        this.name = name;
        this.keywords=keywords;
        this.description=description;

    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
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
    public void appendKeywords(String name){
        this.keywords.add(name);
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