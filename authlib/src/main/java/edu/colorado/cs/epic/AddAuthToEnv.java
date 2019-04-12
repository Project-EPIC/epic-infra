package edu.colorado.cs.epic;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.auth.FirebaseAuthenticator;
import edu.colorado.cs.epic.auth.FirebaseAuthorizator;
import edu.colorado.cs.epic.health.FirebaseAccessHealthCheck;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.io.IOException;

/**
 * Created by admin on 11/4/19.
 */
public class AddAuthToEnv {

    public static void register(Environment environment) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp.initializeApp(options);
        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<FirebaseUser>()
                        .setAuthenticator(new FirebaseAuthenticator())
                        .setAuthorizer(new FirebaseAuthorizator())
                        .setPrefix("Bearer")
                        .buildAuthFilter()));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(FirebaseUser.class));
        environment.healthChecks().register("firebase", new FirebaseAccessHealthCheck());

    }
}
