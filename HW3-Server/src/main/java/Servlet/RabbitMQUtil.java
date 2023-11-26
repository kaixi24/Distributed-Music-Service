package Servlet;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQUtil {
  public static final String QUEUE_NAME = "album_reviews_queue";

  public static Channel createChannel() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost"); // 修改为你的 RabbitMQ 服务器地址
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    return channel;
  }
}

