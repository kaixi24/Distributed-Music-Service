package Servlet;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet(name = "Servlet.AlbumReviewServlet", value = "/review/*")
public class AlbumReviewServlet extends HttpServlet {
  private static final Gson gson = new Gson();
  private RMQChannelPool channelPool;
  private ExecutorService consumerService;
  private final String hostname = "34.222.124.45"; // Consider fetching from config
  private final int numOfConsumer = 250; // Consider fetching from config

  @Override
  public void init() throws ServletException {
    setupConsumerService();
  }

  private void setupConsumerService() throws ServletException {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(hostname);
      com.rabbitmq.client.Connection connection = factory.newConnection();
      RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
      channelPool = new RMQChannelPool(300, channelFactory);
      consumerService = Executors.newFixedThreadPool(numOfConsumer);
      for (int i = 0; i < numOfConsumer; i++) {
        consumerService.submit(new Thread(new ReviewRunnable(channelPool)));
      }
    } catch (Exception e) {
      logError("Failed to connect to RabbitMQ", e);
      throw new ServletException("Failed to connect to RabbitMQ", e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    String urlPath = req.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isPostUrlValid(urlParts)) {
      sendErrorResponse(res, HttpServletResponse.SC_NOT_FOUND, "Invalid request format");
      return;
    }

    try {
      processPostRequest(res, urlParts);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isPostUrlValid(String[] urlParts) {
    return urlParts.length == 3 && (urlParts[1].equals("like") || urlParts[1].equals("dislike"));
  }

  private void processPostRequest(HttpServletResponse res, String[] urlParts) throws Exception {
    String albumId = urlParts[2];
    String likeAction = urlParts[1];

    Channel rabbitMQChannel = null;
    try {
      String message = albumId + "," + likeAction;
      rabbitMQChannel = channelPool.borrowObject();
      rabbitMQChannel.basicPublish("", "reviewQueue", null, message.getBytes("UTF-8"));
      res.getWriter().write(gson.toJson(new errorMsg("Review update request submitted")));
    } catch (IOException e) {
      logError("Error submitting review update", e);
      sendErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error submitting review update");
    } finally {
      if (rabbitMQChannel != null) {
        channelPool.returnObject(rabbitMQChannel);
      }
    }
  }

  private void sendErrorResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
    res.setStatus(statusCode);
    res.getWriter().write(gson.toJson(new errorMsg(message)));
  }

  private void logError(String message, Exception e) {
    // Log the error using a logging framework
  }

  @Override
  public void destroy() {
    if (consumerService != null && !consumerService.isShutdown()) {
      consumerService.shutdownNow();
    }
  }

  private class errorMsg {
    private final String message;

    public errorMsg(String message) {
      this.message = message;
    }

  }
}

