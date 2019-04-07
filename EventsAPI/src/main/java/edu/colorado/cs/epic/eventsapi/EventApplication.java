package edu.colorado.cs.epic.eventsapi;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import edu.colorado.cs.epic.eventsapi.api.User;
import edu.colorado.cs.epic.eventsapi.auth.FirebaseAuthenticator;
import edu.colorado.cs.epic.eventsapi.auth.FirebaseAuthorizator;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.core.KubernetesController;
import edu.colorado.cs.epic.eventsapi.health.KubernetesConnectionHealthCheck;
import edu.colorado.cs.epic.eventsapi.resource.EventResource;
import edu.colorado.cs.epic.eventsapi.resource.RootResource;
import edu.colorado.cs.epic.eventsapi.tasks.SyncEventsTask;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.*;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.*;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.util.Config;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
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
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();
        FirebaseApp.initializeApp(options);

        ApiClient client = Config.defaultClient();
        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        final DatabaseController dbController = new DatabaseController(jdbi);
        final KubernetesController k8sController = new KubernetesController(client, configuration.getKafkaServers(), configuration.getTweetStoreVersion(), configuration.getNamespace(), configuration.getFirehoseConfigMapName());


        if (configuration.getProduction()) {
            environment.jersey().register(new AuthDynamicFeature(
                    new OAuthCredentialAuthFilter.Builder<User>()
                            .setAuthenticator(new FirebaseAuthenticator())
                            .setAuthorizer(new FirebaseAuthorizator())
                            .setPrefix("Bearer")
                            .buildAuthFilter()));

            environment.jersey().register(RolesAllowedDynamicFeature.class);
            //If you want to use @Auth to inject a custom Principal type into your resource
            environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
        }


        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");


        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");


        environment.healthChecks().register("kubernetes", new KubernetesConnectionHealthCheck(client));

        SyncEventsTask task = new SyncEventsTask(k8sController, dbController);
        environment.admin().addTask(task);
        environment.jersey().register(new EventResource(dbController, task));
        environment.jersey().register(new RootResource());


    }
}