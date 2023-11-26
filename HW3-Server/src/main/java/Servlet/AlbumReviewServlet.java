package Servlet;


import com.rabbitmq.client.Channel;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet(name = "Servlet.AlbumReviewServlet", value = "/review")
public class AlbumReviewServlet extends HttpServlet {
  private Gson gson = new Gson();

  public class AlbumReview {
    public String albumId;
    public String userId;
    public String reviewType; // "like" or "dislike"
    public long timestamp;

    AlbumReview(String albumId, String userId, String reviewType, long timestamp){
      this.albumId = albumId;
      this.userId = userId;
      this.reviewType = reviewType;
      this.timestamp = timestamp;
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    // 解析请求到 AlbumReview 对象
    AlbumReview review = gson.fromJson(req.getReader(), AlbumReview.class);

    try {
        Channel channel = RabbitMQUtil.createChannel();
        String message = gson.toJson(review);
        channel.basicPublish("", RabbitMQUtil.QUEUE_NAME, null, message.getBytes());
        res.setStatus(HttpServletResponse.SC_ACCEPTED);
    }catch (Exception e) {
        e.printStackTrace();
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
