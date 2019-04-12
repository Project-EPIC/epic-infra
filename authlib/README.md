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
            <groupId>edu.colorad.cs.epic</groupId>
            <artifactId>authlib</artifactId>
            <version>1.0.0</version>
        </dependency>
```

