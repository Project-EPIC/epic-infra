# Authentication library for Project EPIC

This contains the library to do authentication and authorization for all Project EPIC microservices.

This library needs to be installed before using. 

## Install on local service

Requirements: `mvn`

- `cd authlib`
- `mvn install`
- Add following snippet to your dependencies on `pom.xml`:

```xml
        <dependency>
            <groupId>edu.colorado.cs.epic</groupId>
            <artifactId>authlib</artifactId>
            <version>1.0.0</version>
        </dependency>
```

## Install on DropWizard

To install, add the following code on your Application run method:

```java
AddAuthToEnv.register(environment);
```

Where `environment` is your Environment parameter. To make testing easier, you can add a production variable on your configuration file such that it can be turned on and off without needing to recompile. Example:

```java
if (configuration.getProduction()) {
   AddAuthToEnv.register(environment);
}

```
### Authentificating a request from a client

Include an Authorization header to the request with the following format: `Bearer ACCESS_TOKEN`. Where `ACCESS_TOKEN` is the jwt obtained when logged in on Firebase.


### CORS specification

Make sure the Authorization header is allowed in your CORS configuration.

```java
cors.setInitParameter("allowedOrigins", "*");
cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");
```

### Protect resources

Accepted annotations to protect your resources:

- `@RolesAllowed("ADMIN")`: Protects method against users logged in but not authorized by an internal member.
- `@PermitAll`: Checks if user exists and is logged in. Any user logged in is allowed to access the resource.

### Get User on resource method

To access the logged in user from a resource method, you can add the following parameter:

```java
@Auth FirebaseUser user
```

More information available: https://www.dropwizard.io/1.3.9/docs/manual/auth.html
