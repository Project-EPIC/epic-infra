package eventapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@JsonSnakeCase
public class Event {
    @NotEmpty
    private String name;
    @NotEmpty
    private List<String> keywords = new ArrayList<String>();
    @NotEmpty
    private String description;
    private String normalizedName = null;
    @NotEmpty
    private String status = "ACTIVE";

    @NotNull
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());


    public Event() {
        // Jackson deserialization
    }

    public Event(String name, List<String> keywords, String description) {
        this.name = name;
        this.keywords = keywords;
        this.description = description;
        this.normalizedName = normalizeName(name);
        this.status = "ACTIVE";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    @JsonProperty
    public String getNormalizedName() {
        return normalizedName;
    }

    @JsonProperty
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
        this.normalizedName = normalizeName(name);
    }

    @JsonProperty
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void appendKeywords(String name) {
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
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty
    public void setCreatedAtStr(String createdAt) {
        this.createdAt = Timestamp.valueOf(createdAt);
    }

    @JsonProperty
    public String getCreatedAtStr() {
        return this.createdAt.toString();
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }


    public static String normalizeName(String name) {
        return name.replaceAll("\\s+", "-")
                .replaceAll("[^-a-zA-Z0-9]", "");
    }
}