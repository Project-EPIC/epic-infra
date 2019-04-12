package edu.colorado.cs.epic.tweetsapi;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.tweetsapi.health.GoogleCloudStorageHealthCheck;
import edu.colorado.cs.epic.tweetsapi.resource.RootResource;
import edu.colorado.cs.epic.tweetsapi.resource.TweetResource;
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

public class TweetsApplication extends Application<TweetsConfiguration> {
    public static void main(String[] args) throws Exception {
        new TweetsApplication().run(args);
    }

    @Override
    public String getName() {
        return "tweets";
    }

    @Override
    public void initialize(final Bootstrap<TweetsConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(TweetsConfiguration configuration, Environment environment) throws IOException {

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        if (configuration.getProduction()) {
            AddAuthToEnv.register(environment);
        }

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("epic-collect");
        environment.healthChecks().register("gcloudstorage", new GoogleCloudStorageHealthCheck(bucket));

        environment.jersey().register(new RootResource());
        environment.jersey().register(new TweetResource(bucket));


    }
}
