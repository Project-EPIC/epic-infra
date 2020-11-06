package edu.colorado.cs.epic.tweetsapi;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.tweetsapi.api.StorageIndex;
import edu.colorado.cs.epic.tweetsapi.health.GoogleCloudStorageHealthCheck;
import edu.colorado.cs.epic.tweetsapi.resource.RootResource;
import edu.colorado.cs.epic.tweetsapi.resource.TranslateResource;
import edu.colorado.cs.epic.tweetsapi.resource.TweetResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

public class TweetsApplication extends Application<TweetsConfiguration> {

    private final int MS_IN_DAY = 86400000;
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

        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        AddAuthToEnv.register(environment, configuration.getProduction());

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD,POST");

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket tweetStorage = storage.get("epic-collect");
        StorageIndex storageIndex = new StorageIndex(tweetStorage, jdbi);

        environment.healthChecks().register("gcloudstorage", new GoogleCloudStorageHealthCheck(tweetStorage));

        environment.jersey().register(new RootResource());
        environment.jersey().register(new TweetResource(storageIndex));
        environment.jersey().register(new TranslateResource(configuration.getProjectId()));

        Timer timer = new Timer();
        TimerTask updateStorageIndexTask = new TimerTask() {
            @Override
            public void run() {
                storageIndex.updateAllIndexes();
            }
        };
        // Starting tomorrow, update all event storage indexes every day
        timer.schedule(updateStorageIndexTask, MS_IN_DAY, MS_IN_DAY);
    }
}
