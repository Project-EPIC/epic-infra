package eventapi.resource;

import eventapi.representation.DatabaseProperties;
import eventapi.representation.Event;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.Properties;

@Path("/events")
public class EventResources {
    private DatabaseProperties postgres;
    public EventResources(DatabaseProperties postgres) {
        this.postgres=postgres;
    }

    @Path("/create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response.ResponseBuilder createEvent(Event event) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", postgres.getUsername());
        props.setProperty("password",postgres.getPassword());
        try {
            Connection conn = DriverManager.getConnection(postgres.getUrl(), props);
            Statement stmnt = conn.createStatement();
            String sql = "INSERT INTO events (name, description ) VALUES ( '" + event.getName() + "','"+event.getDescription()+"')";
            System.out.println(sql);
            stmnt.executeUpdate(sql);
            String SQL = "INSERT INTO keywords (event_name,keyword) VALUES (?,?)";
            PreparedStatement statement = conn.prepareStatement(SQL);
            for (String keyword : event.getKeywords()) {
                statement.setString(1, event.getName());
                statement.setString(2, keyword);
                System.out.println(keyword);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return Response.ok(event,"Event created");
    }

}