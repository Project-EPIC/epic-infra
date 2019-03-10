package eventapi.representation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseProperties {
    String url;
    String password;
    String username;
    String ipAddress;

    public DatabaseProperties(String url, String password, String username) {
        this.url = url;
        this.password = password;
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", this.getUsername());
        props.setProperty("password", this.getPassword());
        Connection conn = DriverManager.getConnection(this.getUrl(), props);
        return  conn;
    }
}
