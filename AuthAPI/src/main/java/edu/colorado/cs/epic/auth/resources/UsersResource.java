package edu.colorado.cs.epic.auth.resources;

import com.google.common.collect.Streams;
import com.google.firebase.auth.*;
import edu.colorado.cs.epic.api.FirebaseUser;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Path("/users/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersResource {
    private final Logger logger;


    public UsersResource() {


        this.logger = Logger.getLogger(UsersResource.class.getName());
    }


    @GET
    @RolesAllowed("ADMIN")
    public List<FirebaseUser> getUsers() {
        try {
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            return Streams.stream(page.iterateAll())
                    .map(FirebaseUser::new)
                    .collect(Collectors.toList());
        } catch (FirebaseAuthException e) {
            logger.error("Firebase failed", e);
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);

        }

    }

    @PUT
    @Path("/{uid}/admin")
    @RolesAllowed("ADMIN")
    public Response setAdmin(@PathParam("uid") String uid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("admin", true);
        try {
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            return Response.ok(new FirebaseUser(FirebaseAuth.getInstance().getUser(uid))).build();
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    @PUT
    @Path("/{uid}/enable")
    @RolesAllowed("ADMIN")
    public Response enableUser(@PathParam("uid") String uid) {

        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                    .setDisabled(false);
            UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);

            return Response.ok(new FirebaseUser(userRecord)).build();

        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    @PUT
    @Path("/{uid}/disable")
    @RolesAllowed("ADMIN")
    public Response disableUser(@PathParam("uid") String uid) {

        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                    .setDisabled(true);
            UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
            return Response.ok(new FirebaseUser(userRecord)).build();

        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }


}