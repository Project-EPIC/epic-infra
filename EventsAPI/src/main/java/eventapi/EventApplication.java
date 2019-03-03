package eventapi;

import eventapi.representation.DatabaseProperties;
import eventapi.resource.CreateEventResoure;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

import java.util.Map;

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
        DatabaseProperties db=new DatabaseProperties();
        db.setUrl(configuration.getDatabaseurl());
        db.setPassword(configuration.getPassword());
        db.setUsername(configuration.getUsername());
        final CreateEventResoure resource = new CreateEventResoure(db);

        environment.jersey().register(resource);
    }
}