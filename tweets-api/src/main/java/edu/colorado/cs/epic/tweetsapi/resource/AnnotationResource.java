package edu.colorado.cs.epic.tweetsapi.resource;

import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import edu.colorado.cs.epic.tweetsapi.core.DatabaseController;
import io.dropwizard.jersey.params.DateTimeParam;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/annotation/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnnotationResource {
    private final Logger logger;
    private DatabaseController annotationsdb;

    public AnnotationResource(DatabaseController annotationsdb){
        logger=Logger.getLogger(AnnotationResource.class.getName());
        this.annotationsdb=annotationsdb;
    }

    @POST
    public Response annotateTweet(TweetAnnotation annotation, @HeaderParam("Authorization") String authString){
        System.out.println(authString);
        annotationsdb.addAnnotations(annotation);
        return Response.ok().build();
    }

    @GET
    public Response returnAnnotations(@QueryParam("tweetID") List<String> tweet_id, @QueryParam("eventName") String event_name){

        List<TweetAnnotation> tweets= annotationsdb.getAnnotations(tweet_id, event_name);

        return Response.ok().entity(tweets).build();
    }


}
