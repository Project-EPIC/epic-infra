package edu.colorado.cs.epic.tweetsapi.resource;


import com.google.cloud.translate.v3beta1.LocationName;
import com.google.cloud.translate.v3beta1.TranslateTextRequest;
import com.google.cloud.translate.v3beta1.TranslateTextResponse;
import com.google.cloud.translate.v3beta1.TranslationServiceClient;

import edu.colorado.cs.epic.tweetsapi.api.TranslateRequest;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Path("/tweets/translate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class TranslateResource {

    private final Logger logger;
    private final TranslationServiceClient translationServiceClient;
    private final LocationName locationName;

    public TranslateResource(String projectId) throws IOException {
        this.logger = Logger.getLogger(TweetResource.class.getName());

        this.translationServiceClient = TranslationServiceClient.create();
        this.locationName = LocationName.newBuilder().setProject(projectId).setLocation("global").build();

    }

    @POST
    public JSONObject translateText(@NotNull @Valid TranslateRequest request) {
        TranslateTextRequest translateTextRequest =
                TranslateTextRequest.newBuilder()
                        .setParent(locationName.toString())
                        .setMimeType("text/plain")
                        .setSourceLanguageCode(request.getSource())
                        .setTargetLanguageCode(request.getTarget())
                        .addContents(request.getText())
                        .build();
        TranslateTextResponse response = translationServiceClient.translateText(translateTextRequest);

        JSONObject jsonResp = new JSONObject();
        jsonResp.put("source", request.getSource());
        jsonResp.put("target", request.getTarget());
        jsonResp.put("original", request.getText());
        jsonResp.put("translated", response.getTranslationsList().get(0).getTranslatedText());
        return jsonResp;
    }

}
