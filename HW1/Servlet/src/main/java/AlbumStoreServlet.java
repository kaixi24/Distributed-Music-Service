import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import com.google.gson.Gson;

@WebServlet(name = "AlbumStoreServlet", value = "/albums/*")
public class AlbumStoreServlet extends HttpServlet {
  private static final Gson GSON = new Gson();

  private static class ImageMetaData {
    private final String albumID;
    private final String imageSize;

    ImageMetaData(String albumID, String imageSize) {
      this.albumID = albumID;
      this.imageSize = imageSize;
    }
  }

  private static class AlbumInfo {
    private final String artist;
    private final String title;
    private final String year;

    AlbumInfo(String artist, String title, String year) {
      this.artist = artist;
      this.title = title;
      this.year = year;
    }
  }

  private static class ErrorMsg {
    private final String errorMsg;

    ErrorMsg(String msg) {
      this.errorMsg = msg;
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");

    InputStream inputStream = req.getInputStream();
    int size = inputStream.readAllBytes().length;

    res.setStatus(HttpServletResponse.SC_OK);
    ImageMetaData imageData = new ImageMetaData("123", String.valueOf(size));
    PrintWriter out = res.getWriter();
    out.print(GSON.toJson(imageData));
    out.flush();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    String urlPath = req.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      sendError(res, "Missing parameters", HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isGetUrlValid(urlParts)) {
      sendError(res, "You need to specify album id", HttpServletResponse.SC_NOT_FOUND);
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      AlbumInfo album1 = new AlbumInfo("Adele", "Hello", "2015");
      PrintWriter out = res.getWriter();
      out.print(GSON.toJson(album1));
      out.flush();
    }
  }

  private boolean isGetUrlValid(String[] urlPath) {
    return urlPath.length == 2 && !urlPath[1].isEmpty();
  }

  private void sendError(HttpServletResponse res, String message, int statusCode) throws IOException {
    res.setStatus(statusCode);
    ErrorMsg errorMsg = new ErrorMsg(message);
    PrintWriter out = res.getWriter();
    out.print(GSON.toJson(errorMsg));
    out.flush();
  }
}


