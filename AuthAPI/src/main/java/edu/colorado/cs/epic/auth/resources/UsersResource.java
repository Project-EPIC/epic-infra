package edu.colorado.cs.epic.auth.resources;

import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.ListUsersPage;
import edu.colorado.cs.epic.auth.api.User;
import org.apache.log4j.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Path("/users/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersResource {
    private final Logger logger;


    public UsersResource() {


        this.logger = Logger.getLogger(UsersResource.class.getName());
    }




    @GET
    @PermitAll
    public List<User> getUsers() {
        try {
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            List<User> users = new ArrayList<>();
            for (ExportedUserRecord user : page.iterateAll()) {
                users.add(new User(user));
            }
            return users;
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
            return Response.ok(new User(FirebaseAuth.getInstance().getUser(uid))).build();
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }



   
}