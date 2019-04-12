package edu.colorad.cs.epic;

import edu.colorad.cs.epic.api.FirebaseUser;
import edu.colorad.cs.epic.auth.FirebaseAuthenticator;
import edu.colorad.cs.epic.auth.FirebaseAuthorizator;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

/**
 * Created by admin on 11/4/19.
 */
public class AddAuthToEnv {

    public static void register(Environment environment) {
        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<FirebaseUser>()
                        .setAuthenticator(new FirebaseAuthenticator())
                        .setAuthorizer(new FirebaseAuthorizator())
                        .setPrefix("Bearer")
                        .buildAuthFilter()));

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(FirebaseUser.class));

    }
}
