package eventapi.resource;

import com.codahale.metrics.health.HealthCheck;
import eventapi.representation.DatabaseProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseHealthCheck extends HealthCheck {

    private final DatabaseProperties db;

    public DatabaseHealthCheck(DatabaseProperties db) {
        this.db = db;
    }

    protected Result check() throws Exception {
        try {
            Connection conn = db.getConnection();
            Statement stmnt = conn.createStatement();
            String sql = "SELECT * from events LIMIT 10";
            ResultSet rs = stmnt.executeQuery(sql);
        }catch (Exception e){
            return Result.unhealthy("Cannot connect to database");
        }
        return Result.healthy();
    }
}