import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet(name = "AlbumStoreServlet", value = "/albums/*")
public class AlbumStoreServlet extends HttpServlet {

  private static final Gson gson = new Gson();

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");

    // Validate the content type
    if (!ServletFileUpload.isMultipartContent(req)) {
      sendError(res, "Content type is not multipart/form-data", HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    AlbumInfo albumData = null;
    byte[] imageBytes = null;

    // Parse the request to extract file data
    try {
      List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
      for (FileItem item : items) {
        if (item.isFormField() && "albumData".equals(item.getFieldName())) {
          albumData = gson.fromJson(item.getString(), AlbumInfo.class);
        } else if (!item.isFormField() && "image".equals(item.getFieldName())) {
          imageBytes = item.get();
        }
      }
    } catch (FileUploadException e) {
      sendError(res, "File upload failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    // Validate the extracted data
    if (albumData == null || imageBytes == null) {
      sendError(res, "Invalid album data or image", HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Save album and image to database and respond with the generated album ID
    try {
      int albumId = AlbumDatabase.saveAlbum(albumData, imageBytes);
      ImageMetaData imageData = new ImageMetaData(albumId, imageBytes.length);
      sendResponse(res, imageData, HttpServletResponse.SC_OK);
    } catch (Exception e) {
      sendError(res, "Database operation failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");
    String albumId = req.getPathInfo().substring(1); // Extract album ID from the URL path

    if (albumId.isEmpty()) {
      sendError(res, "Album ID is missing", HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Fetch album data from the database and respond
    try {
      AlbumInfo album = AlbumDatabase.fetchAlbum(albumId);
      if (album != null) {
        sendResponse(res, album, HttpServletResponse.SC_OK);
      } else {
        sendError(res, "Album not found", HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (Exception e) {
      sendError(res, "Database operation failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  // Helper methods to handle response and error
  private void sendResponse(HttpServletResponse res, Object data, int status) throws IOException {
    res.setStatus(status);
    try (PrintWriter out = res.getWriter()) {
      out.print(gson.toJson(data));
    }
  }

  private void sendError(HttpServletResponse res, String errorMessage, int status) throws IOException {
    res.setStatus(status);
    try (PrintWriter out = res.getWriter()) {
      out.print(gson.toJson(new ErrorMsg(errorMessage)));
    }
  }

}

