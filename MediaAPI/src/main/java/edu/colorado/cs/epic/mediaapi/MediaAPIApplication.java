package edu.colorado.cs.epic.mediaapi;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.mediaapi.health.GoogleCloudStorageHealthCheck;
import edu.colorado.cs.epic.mediaapi.resources.MediaResource;
import edu.colorado.cs.epic.mediaapi.resources.RootResource;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;

public class MediaAPIApplication extends Application<MediaAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MediaAPIApplication().run(args);
    }

    @Override
    public String getName() {
        return "MediaAPI";
    }

    @Override
    public void initialize(final Bootstrap<MediaAPIConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final MediaAPIConfiguration configuration, final Environment environment) throws IOException {



        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        AddAuthToEnv.register(environment, configuration.getProduction());

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("epic-analysis-results");
        environment.healthChecks().register("gcloudstorage", new GoogleCloudStorageHealthCheck(bucket));

        environment.jersey().register(new RootResource());
        environment.jersey().register(new MediaResource(bucket));
    }

}
