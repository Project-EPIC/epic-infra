package edu.colorado.cs.epic.tweetsapi.resource;

import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import edu.colorado.cs.epic.tweetsapi.core.DatabaseController;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.params.DateTimeParam;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.Uri;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path("/annotation/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AnnotationResource {
    private final Logger logger;
    private DatabaseController annotationsdb;

    public AnnotationResource(DatabaseController annotationsdb){
        logger=Logger.getLogger(AnnotationResource.class.getName());
        this.annotationsdb=annotationsdb;
    }

    @POST
    public Response annotateTweet(TweetAnnotation annotation, @Auth Optional<FirebaseUser> user){
        if (user.isPresent()){
            annotation.setAuthUser(user.get().getEmail());
        }
        annotationsdb.addAnnotations(annotation);
        return Response.created(URI.create("")).entity(annotation).build();
    }

    @GET
    public List<TweetAnnotation>  returnAnnotations(@QueryParam("tweetID") List<String> tweet_id, @QueryParam("eventName") String event_name){
        return annotationsdb.getAnnotations(tweet_id, event_name);
    }


}
