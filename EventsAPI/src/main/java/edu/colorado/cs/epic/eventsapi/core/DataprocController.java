package edu.colorado.cs.epic.eventsapi.core;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.dataproc.Dataproc;
import com.google.api.services.dataproc.model.InstantiateWorkflowTemplateRequest;
import com.google.auth.oauth2.GoogleCredentials;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Created by admin on 9/4/19.
 */
public class DataprocController {

    private final String projectId;
    private final String region;
    private final String templateName;

    public DataprocController(String projectId, String region, String templateName) {
        this.projectId = projectId;
        this.region = region;
        this.templateName = templateName;
    }

    public void startCollectTemplate(String eventName) throws ExecutionException, InterruptedException, IOException {
        Date curentDate = new Date();

        String dateName = new SimpleDateFormat("yyyy-MM-dd-HH").format(curentDate);
        String dateFolder = new SimpleDateFormat("yyyy/MM/dd/HH").format(curentDate);
        GoogleCredential credentials = GoogleCredential.getApplicationDefault();
        if (credentials.createScopedRequired()) {
            credentials = credentials.createScoped(
                            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform")
            );
        }


        Dataproc dataproc = new Dataproc.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credentials)
                .build();

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("INPUT_JSON", String.format("gs://epic-collect/%s/*/*/*/*/*", eventName));
        parameters.put("OUTPUT_FOLDER", String.format("gs://epic-analysis-results/spark/mentions/%s/%s", eventName, dateFolder));


        InstantiateWorkflowTemplateRequest request = new InstantiateWorkflowTemplateRequest();
        request.setRequestId(String.format("%s-%d", dateName, eventName.hashCode()));
        request.setParameters(parameters);

        String identifier = String.format("projects/%s/regions/%s/workflowTemplates/%s", projectId, region, templateName);
        dataproc.projects().regions().workflowTemplates().instantiate(identifier, request).execute();


    }
}
