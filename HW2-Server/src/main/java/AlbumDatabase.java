import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Static utility class to handle database operations
public class AlbumDatabase {

  public static int saveAlbum(AlbumInfo albumInfo, byte[] imageBytes) throws SQLException {
    String insertSQL = "INSERT INTO albums (artist, title, year_time, image) VALUES (?, ?, ?, ?)";

    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setString(1, albumInfo.artist);
      stmt.setString(2, albumInfo.title);
      stmt.setString(3, albumInfo.year);
      stmt.setBytes(4, imageBytes);

      int affectedRows = stmt.executeUpdate();

      if (affectedRows == 0) {
        throw new SQLException("Creating album failed, no rows affected.");
      }

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getInt(1);
        } else {
          throw new SQLException("Creating album failed, no ID obtained.");
        }
      }
    }
  }

  public static AlbumInfo fetchAlbum(String albumId) throws SQLException {
    String selectSQL = "SELECT artist, title, year_time FROM albums WHERE id = ?";

    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement stmt = connection.prepareStatement(selectSQL)) {

      stmt.setInt(1, Integer.parseInt(albumId));

      try (ResultSet resultSet = stmt.executeQuery()) {
        if (resultSet.next()) {
          String artist = resultSet.getString("artist");
          String title = resultSet.getString("title");
          String year = resultSet.getString("year");

          return new AlbumInfo(artist, title, year);
        } else {
          return null; // Album not found
        }
      }
    }
  }
}