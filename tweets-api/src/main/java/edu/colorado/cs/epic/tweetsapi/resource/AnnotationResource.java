package edu.colorado.cs.epic.tweetsapi.resource;

import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.tweetsapi.api.AnnotatedTags;
import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import edu.colorado.cs.epic.tweetsapi.core.DatabaseController;
import org.apache.log4j.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import io.dropwizard.auth.*;
import java.util.Optional;

@Path("/annotation")
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
    @GET
    @Path("/test")
    public Response test(){
        return Response.ok().build();
    }

    @DELETE
    public Response deleteTag(AnnotatedTags annotation, @Auth Optional<FirebaseUser> user){
        user.ifPresent(firebaseUser -> annotation.setAuthUser(firebaseUser.getEmail()));
        annotationsdb.deleteAnnotations(annotation);
        return Response.ok().build();
    }

    @POST
    public Response annotateTweet(TweetAnnotation annotation, @Auth Optional<FirebaseUser> user){
        user.ifPresent(firebaseUser -> annotation.setAuthUser(firebaseUser.getEmail()));
        annotationsdb.addAnnotations(annotation);
        return Response.ok().build();
    }

    @GET
    public List<AnnotatedTags>  returnAnnotations(@QueryParam("tweetID") List<String> tweet_id, @QueryParam("eventName") String event_name, @Auth Optional<FirebaseUser> user){
        return annotationsdb.getAnnotations(tweet_id, event_name);
    }


}
