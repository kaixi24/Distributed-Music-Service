package Servlet;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlbumReviewWorker {
  private static final Logger logger = LoggerFactory.getLogger(AlbumReviewWorker.class);
  private static RMQChannelPool channelPool;
  private static String queueName = "reviewQueue";

  public AlbumReviewWorker(String hostname, int poolSize, String queueName) {
    AlbumReviewWorker.queueName = queueName;
    setupChannelPool(hostname, poolSize);
  }

  private void setupChannelPool(String hostname, int poolSize) {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(hostname);
      Connection connection = factory.newConnection();
      RMQChannelFactory rmqFactory = new RMQChannelFactory(connection);
      channelPool = new RMQChannelPool(poolSize, rmqFactory);
      declareQueue();
    } catch (Exception e) {
      logger.error("Error setting up channel pool: ", e);
    }
  }

  private void declareQueue() {
    try (Channel channel = channelPool.borrowObject()) {
      channel.queueDeclare(queueName, false, false, false, null);
    } catch (Exception e) {
      logger.error("Error declaring queue: ", e);
    }
  }

  public void startConsumer() {
    try {
      Channel channel = channelPool.borrowObject();
      try {
        channel.basicQos(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          String[] messageParts = message.split(",");
          // Process the message
          // Example: ReviewServlet.postReviewFromDatabase(messageParts[0], messageParts[1]);
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
      } catch (Exception e) {
        logger.error("Error in startConsumer: ", e);
      } finally {
        channelPool.returnObject(channel);
      }
    } catch (Exception e) {
      logger.error("Error borrowing channel from pool: ", e);
    }
  }

}
