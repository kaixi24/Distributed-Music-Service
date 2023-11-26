
import Servlet.AlbumReviewServlet.AlbumReview;
import Servlet.DatabaseUtil;
import Servlet.RabbitMQUtil;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.http.HttpServletResponse;

public class ReviewConsumer {

  public static void main(String[] argv) throws Exception {
    Channel channel = RabbitMQUtil.createChannel();

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      AlbumReview review = new Gson().fromJson(message, AlbumReview.class);

      updateDatabase(review);

      System.out.println(" [x] Received '" + message + "'");
    };
    channel.basicConsume(RabbitMQUtil.QUEUE_NAME, true, deliverCallback, consumerTag -> {
    });
  }

  private static void updateDatabase(AlbumReview review) {
    try (Connection connection = DatabaseUtil.getConnection()) {
      String sql = "INSERT INTO LikeList (albumId, userId, reviewType, timestamp) VALUES (?, ?, ?, ?)";

      try (PreparedStatement stmt = connection.prepareStatement(sql,
          Statement.RETURN_GENERATED_KEYS)) {
        stmt.setString(1, review.albumId);
        stmt.setString(2, review.userId);
        stmt.setString(3, review.reviewType); // "like" or "dislike"
        stmt.setLong(4, review.timestamp);

        stmt.executeUpdate();
      }

    } catch (SQLException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}

