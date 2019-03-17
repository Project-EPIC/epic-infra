package eventapi;

import eventapi.health.KubernetesConnectionHealthCheck;
import eventapi.resource.*;
import io.dropwizard.Application;
import io.dropwizard.configuration.*;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.*;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.util.Config;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;

import javax.servlet.*;
import java.io.IOException;
import java.util.EnumSet;

public class EventApplication extends Application<EventConfiguration> {
    public static void main(String[] args) throws Exception {
        new EventApplication().run(args);
    }

    @Override
    public String getName() {
        return "events";
    }

    @Override
    public void initialize(final Bootstrap<EventConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(EventConfiguration configuration, Environment environment) throws IOException {
        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        ApiClient client = Config.defaultClient();
        final QueryResource queryResource = new QueryResource(client, configuration.getFirehoseConfigMapName(), configuration.getNamespace());
        final FilterResource filterResource = new FilterResource(client, configuration.getKafkaServers(), configuration.getTweetStoreVersion(), configuration.getNamespace());

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");


        environment.healthChecks().register("kubernetes", new KubernetesConnectionHealthCheck(client));
        environment.jersey().register(new EventResource(jdbi, queryResource,filterResource));
        environment.jersey().register(new RootResource());

    }
}