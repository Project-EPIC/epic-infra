package edu.colorado.cs.epic.eventsapi.resource;

import edu.colorado.cs.epic.api.FirebaseUser;
import edu.colorado.cs.epic.eventsapi.api.ExtendedTweetAnnotation;
import edu.colorado.cs.epic.eventsapi.api.TweetAnnotation;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import io.dropwizard.auth.*;

import java.util.Optional;

@Path("/annotations/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AnnotationResource {
    private final Logger logger;
    private DatabaseController annotationsdb;

    public AnnotationResource(DatabaseController annotationsdb) {
        logger = Logger.getLogger(AnnotationResource.class.getName());
        this.annotationsdb = annotationsdb;
    }

    @GET
    public List<TweetAnnotation> returnAllAnnotations(@QueryParam("tweet_id") List<String> tweet_id) {
        List<TweetAnnotation> annotations = annotationsdb.getAllAnnotations(tweet_id);
        if (annotations.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return annotations;
    }

    @GET
    @Path("/{event_name}/")
    public List<TweetAnnotation> returnAnnotations(@QueryParam("tweet_id") List<String> tweet_id, @PathParam("event_name") String event_name) {
        List<TweetAnnotation> annotations = annotationsdb.getAnnotations(tweet_id, event_name);
        if (annotations.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return annotations;
    }

    @GET
    @Path("/{event_name}/{tweet_id}/")
    public List<TweetAnnotation> getTweetTags(@PathParam("event_name") String eventName, @PathParam("tweet_id") String tweetId) {
        List<TweetAnnotation> annotations = annotationsdb.getAnnotations(Collections.singletonList(tweetId), eventName);
        if (annotations.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return annotations;
    }


    @GET
    @Path("/{event_name}/{tweet_id}/{tag}")
    public TweetAnnotation getTag(@PathParam("event_name") String eventName, @PathParam("tweet_id") String tweetId, @PathParam("tag") String tag) {
        Optional<TweetAnnotation> annotation = annotationsdb.getAnnotation(eventName, tweetId, tag);
        if (!annotation.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return annotation.get();
    }


    @DELETE
    @Path("/{event_name}/{tweet_id}/{tag}")
    public Response deleteTag(@PathParam("event_name") String eventName, @PathParam("tweet_id") String tweetId, @PathParam("tag") String tag) {
        annotationsdb.deleteAnnotation(eventName, tweetId, tag);
        return Response.noContent().build();
    }


    @POST
    public Response annotateTweet(ExtendedTweetAnnotation annotation, @Auth Optional<FirebaseUser> user) {
        if (annotationsdb.annotationExists(annotation)){
            logger.info(String.format("Already existing annotation: %s", annotation.getTag()));
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        user.ifPresent(firebaseUser -> annotation.setAuthUser(firebaseUser.getEmail()));
        annotationsdb.addAnnotation(annotation);
        return Response.ok().entity((TweetAnnotation) annotation).build();
    }


}
