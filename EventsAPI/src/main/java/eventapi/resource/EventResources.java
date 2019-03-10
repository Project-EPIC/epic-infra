package eventapi.resource;

import eventapi.representation.DatabaseProperties;
import eventapi.representation.Event;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Path("/events")
public class EventResources {
    private DatabaseProperties postgres;
    public EventResources(DatabaseProperties postgres) {
        this.postgres=postgres;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEvent(Event event) throws SQLException {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"
        try {
            Connection conn = postgres.getConnection();
            Statement stmnt = conn.createStatement();
            event.setNormalizedName(event.getName().replaceAll("\\s+", "-")
                    .replaceAll("[^-a-zA-Z0-9]", ""));
            String sql = "INSERT INTO events (name, description, normalized_name) VALUES ( '" + event.getName() + "','"+event.getDescription()+"','"+event.getNormalizedName()+"')";
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
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@PathParam("id") String id) throws SQLException {
        Connection conn = postgres.getConnection();
        Statement stmnt = conn.createStatement();
        String sql="";
        if(id.compareTo("all")!=0){
            sql="SELECT * from events,keywords where name=event_name and name='"+id+"';";
        }else{
            sql="SELECT * from events,keywords where name=event_name;";
        }
        System.out.println(sql);
        ResultSet rs = stmnt.executeQuery(sql);
        HashMap<String,Event> eventList=new HashMap<String, Event>();
        while(rs.next()){
            Event e=eventList.get(rs.getString("normalized_name"));
            if(e!=null){
                e.appendKeywords(rs.getString("keyword"));
            }else{
                e=new Event();
                e.setName(rs.getString("name"));
                e.setNormalizedName(rs.getString("normalized_name"));
                e.setDescription(rs.getString("description"));
                e.appendKeywords(rs.getString("keyword"));
            }
            eventList.put(rs.getString("normalized_name"), e);
        }
        ArrayList<Event> e=new ArrayList<Event>(eventList.values());
        return Response.ok().entity(e).build();
    }
}