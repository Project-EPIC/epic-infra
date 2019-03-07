package eventapi.resource;

import eventapi.representation.DatabaseProperties;
import eventapi.representation.Event;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.sql.*;
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
    public long createEvent(@QueryParam(value = "name") String name, @QueryParam(value = "description") String description, @QueryParam(value = "keywords") String keywords) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", postgres.getUsername());
        props.setProperty("password",postgres.getPassword());
        try {
            Connection conn = DriverManager.getConnection(postgres.getUrl(), props);
            Statement stmnt = conn.createStatement();
            String sql = "INSERT INTO events (name, description ) VALUES ( '" + name + "','"+description+"')";
            System.out.println(sql);
            stmnt.executeUpdate(sql);
            String SQL = "INSERT INTO keywords (event_name,keyword) VALUES (?,?)";
            PreparedStatement statement = conn.prepareStatement(SQL);
            for (String keyword : keywords.split(",")) {
                statement.setString(1, name);
                statement.setString(2, keyword);
                System.out.println(keyword);
                statement.addBatch();
            }
            statement.executeBatch();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return 0;
        }
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