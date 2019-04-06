package edu.colorado.cs.epic.tweetsapi;

import edu.colorado.cs.epic.tweetsapi.resource.RootResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
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
    public void run(TweetsConfiguration configuration, Environment environment) {

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");


//        environment.healthChecks().register("kubernetes", new KubernetesConnectionHealthCheck(client));

        environment.jersey().register(new RootResource());


    }
}
