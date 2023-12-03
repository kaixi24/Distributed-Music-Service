package Servlet;

import java.util.List;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import com.google.gson.Gson;


@WebServlet(name = "Servlet.AlbumStoreServlet", value = "/albums/*")
public class AlbumStoreServlet extends HttpServlet {
  private static final Gson gson = new Gson();

  class ImageMetaData {
    private String albumID;
    private String imageSize;

    ImageMetaData(String albumID, String imageSize) {
      this.albumID = albumID;
      this.imageSize = imageSize;
    }
  }

  class AlbumInfo {
    private String artist;
    private String title;
    private String year;

    AlbumInfo(String artist, String title, String year) {
      this.artist = artist;
      this.title = title;
      this.year = year;
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");

    if (!ServletFileUpload.isMultipartContent(req)) {
      sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Content type is not multipart/form-data");
      return;
    }

    try {
      processUpload(req, res);
    } catch (FileUploadException e) {
      sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to parse multipart request");
    } catch (SQLException | ClassNotFoundException e) {
      sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
    }
  }

  private void processUpload(HttpServletRequest req, HttpServletResponse res) throws FileUploadException, SQLException, ClassNotFoundException, IOException {
    List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);

    AlbumInfo albumData = null;
    byte[] imageBytes = null;

    for (FileItem item : items) {
      if (item.isFormField() && "albumData".equals(item.getFieldName())) {
        albumData = gson.fromJson(item.getString(), AlbumInfo.class);
      } else if (!item.isFormField() && "image".equals(item.getFieldName())) {
        imageBytes = item.get();
      }
    }

    if (albumData == null || imageBytes == null) {
      sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Data missing");
      return;
    }

    storeAlbumData(albumData, imageBytes, res);
  }

  private void storeAlbumData(AlbumInfo albumData, byte[] imageBytes, HttpServletResponse res) throws SQLException, ClassNotFoundException, IOException {
    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO albums (artist, title, year, image) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, albumData.artist);
      stmt.setString(2, albumData.title);
      stmt.setString(3, albumData.year);
      stmt.setBytes(4, imageBytes);
      stmt.executeUpdate();

      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          int generatedKey = rs.getInt(1);
          int imageSize = imageBytes.length;
          ImageMetaData imageData = new ImageMetaData(String.valueOf(generatedKey), String.valueOf(imageSize));
          sendResponse(res, HttpServletResponse.SC_OK, gson.toJson(imageData));
        }
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");

    String urlPath = req.getPathInfo();
    if (urlPath == null || urlPath.isEmpty() || urlPath.split("/").length != 2) {
      sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
      return;
    }

    String albumId = urlPath.split("/")[1];
    try {
      AlbumInfo album = fetchAlbumFromDatabase(albumId);
      if (album != null) {
        sendResponse(res, HttpServletResponse.SC_OK, gson.toJson(album));
      } else {
        sendError(res, HttpServletResponse.SC_NOT_FOUND, "Album not found");
      }
    } catch (SQLException | ClassNotFoundException e) {
      sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
    }
  }

  private AlbumInfo fetchAlbumFromDatabase(String albumId) throws SQLException, ClassNotFoundException {
    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT artist, title, year FROM albums WHERE id = ?")) {
      statement.setInt(1, Integer.parseInt(albumId));

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return new AlbumInfo(resultSet.getString("artist"), resultSet.getString("title"), resultSet.getString("year"));
        }
      }
    }
    return null;
  }

  private void sendError(HttpServletResponse res, int statusCode, String message) throws IOException {
    res.setStatus(statusCode);
    res.getWriter().write(gson.toJson(new errorMsg(message)));
  }

  private void sendResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
    res.setStatus(statusCode);
    res.getWriter().write(message);
  }

  class errorMsg {
    private final String message;
    public errorMsg(String message) {
      this.message = message;
    }
  }
}


