package edu.colorado.cs.epic.filteringapi;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import edu.colorado.cs.epic.filteringapi.resources.FilteringResource;
import edu.colorado.cs.epic.filteringapi.resources.RootResource;

public class FilteringApplication extends Application<Configuration> {
  public static void main(final String[] args) throws Exception {
    new FilteringApplication().run(args);
  }

  @Override
  public String getName() {
      return "Filtering";
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
      new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
      )
    );
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(new RootResource());
    environment.jersey().register(new FilteringResource());
  }
}
