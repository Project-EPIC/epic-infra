package edu.colorado.cs.epic.geoupdateapi;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import edu.colorado.cs.epic.AddAuthToEnv;
import edu.colorado.cs.epic.geoupdateapi.resources.GeoUpdateResource;
import edu.colorado.cs.epic.geoupdateapi.resources.RootResource;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;

public class GeoUpdateApplication extends Application<GeoUpdateConfiguration> {
  
  public static void main(final String[] args) throws Exception {
    new GeoUpdateApplication().run(args);
  }

  @Override
  public String getName() {
      return "GeoUpdate";
  }

  @Override
  public void initialize(final Bootstrap<GeoUpdateConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
      new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
      )
    );
  }

  @Override
  public void run(GeoUpdateConfiguration configuration, Environment environment) throws IOException {

    final FilterRegistration.Dynamic cors =
        environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    AddAuthToEnv.register(environment, configuration.getProduction());

    // Configure CORS parameters
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

    environment.jersey().register(new RootResource());
    environment.jersey().register(new GeoUpdateResource());
  }
}
