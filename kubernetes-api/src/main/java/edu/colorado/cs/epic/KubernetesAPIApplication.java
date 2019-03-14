package edu.colorado.cs.epic;

import edu.colorado.cs.epic.health.KubernetesConnectionHealthCheck;
import edu.colorado.cs.epic.resources.FilterResource;
import edu.colorado.cs.epic.resources.QueryResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.util.Config;

public class KubernetesAPIApplication extends Application<KubernetesAPIConfiguration> {

    public static void main(final String[] args) throws Exception {
        new KubernetesAPIApplication().run(args);
    }

    @Override
    public String getName() {
        return "firehose-api";
    }

    @Override
    public void initialize(final Bootstrap<KubernetesAPIConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final KubernetesAPIConfiguration configuration,
                    final Environment environment) throws Exception {
        ApiClient client = Config.defaultClient();

        environment.healthChecks().register("kubernetes", new KubernetesConnectionHealthCheck(client));
        environment.jersey().register(new QueryResource(client, configuration.getFirehoseConfigMapName(), configuration.getNamespace()));
        environment.jersey().register(new FilterResource(client, configuration.getKafkaServers(), configuration.getTweetStoreVersion(), configuration.getNamespace()));

    }

}
