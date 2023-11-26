import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.sql.Statement;
import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


@WebServlet(name = "AlbumStoreServlet",value = "/albums/*")
public class AlbumStoreServlet extends HttpServlet {
  private Gson gson = new Gson();

  class imageMetaData {
    private String albumID;
    private String imageSize;
    imageMetaData(String albumID, String imageSize) {
      this.albumID = albumID;
      this.imageSize = imageSize;
    }
  }

  class albumInfo {
    private String artist;
    private String title;
    private String year;

    albumInfo(String artist, String title, String year) {
      this.artist = artist;
      this.title = title;
      this.year = year;
    }

  }

  class errorMsg {
    private String errorMsg;

    public errorMsg (String msg) {
      errorMsg = msg;
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");

    if (!ServletFileUpload.isMultipartContent(req)) {
      throw new ServletException("Content type is not multipart/form-data");
    }

    albumInfo albumData = null;
    byte[] imageBytes = null;

    try {
      List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
      for (FileItem item : items) {
        if (item.isFormField()) {
          if ("albumData".equals(item.getFieldName())) {
            albumData = gson.fromJson(item.getString(), albumInfo.class);
          }
        } else {
          if ("image".equals(item.getFieldName())) {
            imageBytes = item.get();
          }
        }
      }

      if (albumData == null || imageBytes == null) {
        throw new ServletException("Data missing");
      }

      int imageSize = imageBytes.length;

      try (Connection connection = DatabaseUtil.getConnection()) {
        String sql = "INSERT INTO albums (artist, title, year, image) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
          stmt.setString(1, albumData.artist);
          stmt.setString(2, albumData.title);
          stmt.setString(3, albumData.year);
          stmt.setBytes(4, imageBytes);

          stmt.executeUpdate();

          // Retrieve the auto-generated key
          ResultSet rs = stmt.getGeneratedKeys();
          int generatedKey = 0;
          if (rs.next()) {
            generatedKey = rs.getInt(1);
          }

          res.setStatus(HttpServletResponse.SC_OK);
          imageMetaData imageData = new imageMetaData(String.valueOf(generatedKey), String.valueOf(imageSize));
          String str = gson.toJson(imageData);
          PrintWriter out = res.getWriter();
          out.print(str);
          out.flush();
        }
      } catch (SQLException e) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    } catch (FileUploadException e) {
      throw new ServletException("Failed to parse multipart request", e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    String urlPath = req.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("Missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isGetUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      errorMsg getErrorMsgGet = new errorMsg("You need to specify album id");
      res.getWriter().write(this.gson.toJson(getErrorMsgGet));
    } else {
      String albumId = urlParts[1];

      albumInfo album = fetchAlbumFromDatabase(albumId);

      if (album != null) {
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write(this.gson.toJson(album));
      } else {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        errorMsg error = new errorMsg("Album not found");
        res.getWriter().write(this.gson.toJson(error));
      }
    }
  }

  private albumInfo fetchAlbumFromDatabase(String albumId) {
    String sql = "SELECT artist, title, year FROM albums WHERE id = ?"; // Assuming your table has columns: artist, title, year
    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

      statement.setInt(1, Integer.parseInt(albumId));

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String artist = resultSet.getString("artist");
          String title = resultSet.getString("title");
          String year = resultSet.getString("year");

          return new albumInfo(artist, title, year);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace(); // You might want to log the error in a production application
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null; // Return null if album is not found or if there's any exception
  }

  private boolean isGetUrlValid(String[] urlPath) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    if(urlPath.length == 2 && urlPath[1].length() > 0) return true;
    return false;
  }

}



