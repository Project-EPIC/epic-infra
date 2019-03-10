package eventapi;

import com.codahale.metrics.health.HealthCheckRegistry;
import eventapi.representation.DatabaseProperties;
import eventapi.resource.DatabaseHealthCheck;
import eventapi.resource.EventResources;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class EventApplication extends Application<EventConfiguration> {
    public static void main(String[] args) throws Exception {
        new EventApplication().run(args);
    }

    @Override
    public String getName(){
        return "events";
    }
    @Override
    public void run(EventConfiguration configuration, Environment environment) {
        DatabaseProperties db=new DatabaseProperties(configuration.getDatabaseurl(), configuration.getPassword(), configuration.getUsername());
        final EventResources resource = new EventResources(db);

        environment.healthChecks().register("database", new DatabaseHealthCheck(db));
        environment.jersey().register(resource);
    }
}