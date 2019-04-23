package edu.colorado.cs.epic.tweetsapi.resource;

import edu.colorado.cs.epic.tweetsapi.api.TweetAnnotation;
import edu.colorado.cs.epic.tweetsapi.core.DatabaseController;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/annotate/")
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
    public Response annotateTweet(TweetAnnotation annotation){
        annotationsdb.addAnnotations(annotation);
        return Response.ok().build();
    }


}
