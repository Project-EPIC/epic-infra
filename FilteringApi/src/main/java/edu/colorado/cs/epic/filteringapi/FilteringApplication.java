package edu.colorado.cs.epic.filteringapi;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.filteringapi.resources.FilteringResource;
import edu.colorado.cs.epic.filteringapi.resources.RootResource;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;

public class FilteringApplication extends Application<FilteringConfiguration> {
  
  public static void main(final String[] args) throws Exception {
    new FilteringApplication().run(args);
  }

  @Override
  public String getName() {
      return "Filtering";
  }

  @Override
  public void initialize(final Bootstrap<FilteringConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
      new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
      )
    );
  }

  @Override
  public void run(FilteringConfiguration configuration, Environment environment) throws IOException {

    final FilterRegistration.Dynamic cors =
        environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    AddAuthToEnv.register(environment, configuration.getProduction());

    // Configure CORS parameters
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

    environment.jersey().register(new RootResource());
    environment.jersey().register(new FilteringResource());
  }
}
