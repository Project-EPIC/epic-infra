package edu.colorado.cs.epic.geotagapi;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jdbi.v3.core.Jdbi;

import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.geotagapi.health.GoogleCloudStorageHealthCheck;
import edu.colorado.cs.epic.geotagapi.resources.GeoTagResource;
import edu.colorado.cs.epic.geotagapi.resources.RootResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.JdbiFactory;

public class GeoTagAPIApplication extends Application<GeoTagAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new GeoTagAPIApplication().run(args);
    }

    @Override
    public String getName() {
        return "GeoTagAPI";
    }

    @Override
    public void initialize(final Bootstrap<GeoTagAPIConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final GeoTagAPIConfiguration configuration,
                    final Environment environment) throws IOException {
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
        Bucket bucket = storage.get("epic-analysis-results");
        environment.healthChecks().register("gcloudstorage", new GoogleCloudStorageHealthCheck(bucket));

        environment.jersey().register(new RootResource());
        environment.jersey().register(new GeoTagResource(bucket, jdbi));
    }

}
