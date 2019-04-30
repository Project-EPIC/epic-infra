package edu.colorado.cs.epic.eventsapi;



import edu.colorado.cs.epic.AddAuthToEnv;

import edu.colorado.cs.epic.eventsapi.core.BigQueryController;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.core.DataprocController;
import edu.colorado.cs.epic.eventsapi.core.KubernetesController;
import edu.colorado.cs.epic.eventsapi.health.KubernetesConnectionHealthCheck;
import edu.colorado.cs.epic.eventsapi.resource.EventResource;
import edu.colorado.cs.epic.eventsapi.resource.RootResource;
import edu.colorado.cs.epic.eventsapi.tasks.SyncEventsTask;
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
        ApiClient client = Config.defaultClient();
        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        final DatabaseController dbController = new DatabaseController(jdbi);
        final BigQueryController bqController = new BigQueryController(configuration.getCollectBucketName());

        final KubernetesController k8sController = new KubernetesController(client, configuration.getKafkaServers(), configuration.getTweetStoreVersion(), configuration.getNamespace(), configuration.getFirehoseConfigMapName());


        final DataprocController dataprocController = new DataprocController(configuration.getGcloudProjectID(), "global", configuration.getTemplateNameDataproc());


        AddAuthToEnv.register(environment,configuration.getProduction());

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");


        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");


        environment.healthChecks().register("kubernetes", new KubernetesConnectionHealthCheck(client));

        SyncEventsTask task = new SyncEventsTask(k8sController, dbController, dataprocController);
        environment.admin().addTask(task);
        environment.jersey().register(new EventResource(dbController, bqController, task));
        environment.jersey().register(new RootResource());


    }
}