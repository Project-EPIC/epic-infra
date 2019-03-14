package eventapi.representation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private String name;
    private List<String> keywords=new ArrayList<String>();
    private String description;
    private  String normalizedName;
    private String status;
    private Timestamp created_at;
    public Event() {
        // Jackson deserialization
    }

    public Event(String name, List<String> keywords, String description) {
        this.name = name;
        this.keywords=keywords;
        this.description=description;
        this.status="ACTIVE";
        this.created_at=new Timestamp(System.currentTimeMillis());
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
    @JsonProperty
    public String getStatus() { return status; }
    @JsonProperty
    public void setStatus(String status) { this.status = status; }
    @JsonProperty
    public void setCreated_at(String time) { this.created_at=Timestamp.valueOf(time);}
    @JsonProperty
    public void setCreated_at() { this.created_at = new Timestamp(System.currentTimeMillis()); }
    @JsonProperty
    public String getCreated_at() {return this.created_at.toString(); }
}