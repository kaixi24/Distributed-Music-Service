package Servlet;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ReviewRunnable.class);
  private static final String queueName = "reviewQueue";
  private final RMQChannelPool channelPool;

  public ReviewRunnable(RMQChannelPool pool) {
    this.channelPool = pool;
  }

  @Override
  public void run() {
    Channel channel = null;
    try {
      channel = channelPool.borrowObject();
      setupChannel(channel);
      startConsuming(channel);
    } catch (IOException e) {
      logger.error("IOException occurred: ", e);
    } finally {
      safelyReturnChannel(channel);
    }
  }

  private void setupChannel(Channel channel) throws IOException {
    channel.queueDeclare(queueName, false, false, false, null);
    channel.basicQos(50);
  }

  private void startConsuming(Channel channel) {
    try {
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        String[] messageParts = message.split(",");
        if (postReviewFromDatabase(messageParts[0], messageParts[1])) {
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});
    } catch (IOException e) {
      logger.error("Error in consuming messages: ", e);
    }
  }

  private void safelyReturnChannel(Channel channel) {
    if (channel != null) {
      try {
        channelPool.returnObject(channel);
      } catch (Exception e) {
        logger.error("Error returning channel to pool: ", e);
      }
    }
  }

  private boolean postReviewFromDatabase(String albumId, String likeAction) {
    String columnToUpdate = likeAction.equals("like") ? "`like`" : "dislike";
    String sql = "UPDATE albums SET " + columnToUpdate + " = " + columnToUpdate + " + 1 WHERE id = ?";
    try (Connection connection = DatabaseUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, Integer.parseInt(albumId));
      return statement.executeUpdate() > 0;
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("Error updating database: ", e);
      return false;
    }
  }
}
