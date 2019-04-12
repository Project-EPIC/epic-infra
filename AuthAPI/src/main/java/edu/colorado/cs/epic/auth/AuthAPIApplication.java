package edu.colorado.cs.epic.auth;

import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.auth.resources.RootResource;
import edu.colorado.cs.epic.auth.resources.UsersResource;
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

public class AuthAPIApplication extends Application<AuthAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new AuthAPIApplication().run(args);
    }

    @Override
    public String getName() {
        return "AuthAPI";
    }

    @Override
    public void initialize(final Bootstrap<AuthAPIConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final AuthAPIConfiguration configuration,
                    final Environment environment) throws IOException {


        if (configuration.getProduction()) {
            AddAuthToEnv.register(environment);
        }

        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");


        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");


        environment.jersey().register(new UsersResource());
        environment.jersey().register(new RootResource());
    }

}
