package eventapi;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class EventConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    private String kubernetesFiltersApiUrl;

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty
    public String getKubernetesFiltersApiUrl() {
        return kubernetesFiltersApiUrl;
    }

    @JsonProperty
    public void setKubernetesFiltersApiUrl(String kubernetesFiltersApiUrl) {
        this.kubernetesFiltersApiUrl = kubernetesFiltersApiUrl;
    }
}