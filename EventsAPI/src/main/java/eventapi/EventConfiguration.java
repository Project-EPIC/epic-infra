package eventapi;
import com.fasterxml.jackson.annotation.JsonProperty;
import eventapi.representation.DatabaseProperties;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class EventConfiguration extends Configuration {
    private String databaseurl;

    private String username;

    private String password;


    @JsonProperty
    public String getUsername() { return username; }

    @JsonProperty
    public void setUsername(String username) { this.username = username;  }

    @JsonProperty
    public String getPassword() { return password; }

    @JsonProperty
    public void setPassword(String password) { this.password = password; }

    @JsonProperty
    public String getDatabaseurl() {
        return databaseurl;
    }

    @JsonProperty
    public void setDatabaseurl(String databaseurl) { this.databaseurl = databaseurl;  }

//    public DatabaseProperties getDb() { return db; }
}
