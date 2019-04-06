package edu.colorado.cs.epic.tweetsapi.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/tweets/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TweetResource {
    private final Logger logger;

    public TweetResource(){
        this.logger=Logger.getLogger(TweetResource.class.getName());
    }

    @GET
    @Path("/{event_name}/{page_number}/{page_size}")
    public Response getTweets(@PathParam("event_name") String event_name, @PathParam("page_number") String page_numer)
}
