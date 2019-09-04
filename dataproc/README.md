# Google Dataproc Workflow template

To manage batch jobs, we use Google Cloud Dataproc Workflow templates. 

## Create Google Cloud Workflow

The following command should be able to create the workflow with all parameters ready to be triggered by [EventsAPI](../EventsAPI) when needed. 

1. Login in your CLI: `gcloud init`
2. `gcloud dataproc workflow-templates import epic-spark --source workflow.yaml`

You can check the results going to the [web interface of Dataproc](https://console.cloud.google.com/dataproc/workflows/instances).

## Creating new Spark job

Requires: `mvn`, 

1. `mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4`
2. Add Spark dependency and shading plugin to the generated `pom.xml` (see [media-spark pom.xml](../media-spark/pom.xml) as an example)
3. Create Spark job. You should take in an event name as the only argument. This event name should direct the Spark job to execute on an specific event. Make sure that results are also output into separate folders by event.
4. Generate jar: `mvn package`
5. Upload jar from `target` folder to [epic-spark-jars Google Cloud bucket](https://console.cloud.google.com/storage/browser/epic-spark-jars)
6. Add job to [workflow.yml](workflow.yml) under the `jobs` tag. Use this template replacing with your jar file and step id of your job (step id needs to be unique):
```yml
- sparkJob:
    args:
    - gs://epic-historic-tweets/random/*
    mainJarFileUri: gs://epic-spark-jars/YOUR_JAR_FILE.jar
  stepId: YOUR_STEP_ID
```
7. Add your step into the event parameter (see `fields` list). Make sure to replace your `YOUR_STEP_ID` with the step id you set in the previous step.
```yml
  - jobs['YOUR_STEP_ID'].sparkJob.args[0]
```
8. Update workflow: `gcloud dataproc workflow-templates import epic-spark --source workflow.yaml`

