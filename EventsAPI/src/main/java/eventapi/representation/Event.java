package eventapi.representation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Event {
    private long id;
    private String name;
    private List<String> keywords;

    public Event() {
        // Jackson deserialization
    }

    public Event(String name, List<String> keywords) {
        this.name = name;
        this.keywords=keywords;

        //Where do I insert in SQL
    }

    @JsonProperty
    public long getId() {
        //return form sql
        return id;
    }

    @JsonProperty
    public String getName() {
        return name;
    }


    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }
}