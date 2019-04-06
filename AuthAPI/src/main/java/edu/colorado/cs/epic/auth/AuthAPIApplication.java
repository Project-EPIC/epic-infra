package edu.colorado.cs.epic.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuthException;

import edu.colorado.cs.epic.auth.api.User;
import edu.colorado.cs.epic.auth.auth.FirebaseAuthenticator;
import edu.colorado.cs.epic.auth.auth.FirebaseAuthorizator;
import edu.colorado.cs.epic.auth.health.FirebaseAccessHealthCheck;
import edu.colorado.cs.epic.auth.resources.RootResource;
import edu.colorado.cs.epic.auth.resources.UsersResource;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

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
                    final Environment environment) throws IOException, FirebaseAuthException {
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();
        FirebaseApp.initializeApp(options);



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


        environment.jersey().register(new UsersResource());
        environment.jersey().register(new RootResource());

        environment.healthChecks().register("firebase",new FirebaseAccessHealthCheck());

    }

}
