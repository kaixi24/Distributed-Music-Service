import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseUtil {
  private static final HikariConfig config = new HikariConfig();
  private static HikariDataSource ds;

  static {
    initializeDataSource();
  }

  private DatabaseUtil() {}

  private static void initializeDataSource() {
    try {
      // No need to explicitly load the driver class if you are using a JDBC 4.0 driver
      config.setJdbcUrl("jdbc:mysql://database-1.cb2mcojnrxhs.us-west-2.rds.amazonaws.com:3306/mydb4?useSSL=false");
      config.setUsername("admin");
      config.setPassword("xikai123");
      config.setMaximumPoolSize(60); // Set your desired pool size

      // Additional pool configuration can be set here if required

      ds = new HikariDataSource(config);
      System.out.println("Connection pool initialized successfully.");
    } catch (Exception e) {
      System.err.println("Failed to initialize the connection pool: " + e.getMessage());
      throw new RuntimeException("Failed to initialize the connection pool", e);
    }
  }

  public static Connection getConnection() throws SQLException {
    return ds.getConnection();
  }
}
