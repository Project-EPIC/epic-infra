package eventapi.resource;

import eventapi.representation.DatabaseProperties;
import eventapi.representation.Event;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

@Path("/events")
public class CreateEventResoure {
    private DatabaseProperties postgres;
    public CreateEventResoure(DatabaseProperties postgres) {
        this.postgres=postgres;
    }

    @Path("/create")
    @GET
    public Event createEvent(@QueryParam("name") String name) throws SQLException {
        Event e= new Event(name,keywords);
        Properties props = new Properties();
        props.setProperty("user", postgres.getUsername());
        props.setProperty("password",postgres.getPassword());
        //props.setProperty("ssl","true");
        Connection conn = DriverManager.getConnection(postgres.getUrl(), props);
        conn.createStatement();
        return 1;
    }
    @Path("/test")
    @GET
    public long test() throws SQLException {
        //Event e= new Event(name,keywords);
        Properties props = new Properties();
        props.setProperty("user", postgres.getUsername());
        props.setProperty("password",postgres.getPassword());
        //props.setProperty("ssl","true");
        Connection conn = DriverManager.getConnection(postgres.getUrl(), props);
        conn.createStatement();
        return 1;
    }
}