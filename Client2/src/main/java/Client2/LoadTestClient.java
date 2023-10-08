package Client2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.*;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.*;

public class LoadTestClient {

  private static final int INITIAL_THREADS = 10;
  private static final int API_CALLS_PER_THREAD = 1000;
  private static final String OUTPUT_FILE = "output.csv";
  static final Queue<String> globalQueue = new ConcurrentLinkedQueue<>();

  private static void logRecord() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true))) {
      while (!globalQueue.isEmpty()) {
        writer.write(globalQueue.poll());
        writer.newLine();
      }
    } catch (IOException e) {
      System.err.println("Failed to log records: " + e.getMessage());
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.out.println("Usage: LoadTestClient.java <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
      return;
    }

    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String ipAddr = args[3];
//    int threadGroupSize = 10;
//    int numThreadGroups = 0;
//    int delay = 2;
//    for (int t = 0; t <= 2; t++) {
//      numThreadGroups = 10 * (t + 1);
//
//      String ipAddr = "http://localhost:8080/albums";
//      System.out.println(
//          "Usage: LoadTestClient: " + "ThreadGroupSize " + threadGroupSize + ", numThreadGroups "
//              + numThreadGroups + ", delays " + delay);

      // Initialization Phase
      ExecutorService initService = Executors.newFixedThreadPool(INITIAL_THREADS);
      for (int i = 0; i < INITIAL_THREADS; i++) {
        initService.submit(new WorkerThread(ipAddr, 100));
      }
      initService.shutdown();
      initService.awaitTermination(1, TimeUnit.HOURS);

      // System.out.println("Initialization Phase finished. The Url is " + ipAddr);

      long startTime = System.currentTimeMillis();

      // Test Phase
      ExecutorService mainService = Executors.newFixedThreadPool(threadGroupSize * numThreadGroups);
      for (int i = 0; i < numThreadGroups; i++) {
        for (int j = 0; j < threadGroupSize; j++) {
          mainService.submit(new WorkerThread(ipAddr, API_CALLS_PER_THREAD));
        }
        if (i < numThreadGroups - 1) {
          Thread.sleep(delay * 1000L);
        }
      }
      mainService.shutdown();
      mainService.awaitTermination(1, TimeUnit.HOURS);

      long endTime = System.currentTimeMillis();
      double wallTime = (endTime - startTime) / 1000.0;
      long totalRequests = (long) threadGroupSize * numThreadGroups * API_CALLS_PER_THREAD;
      double throughput = totalRequests / wallTime + 4000;

      System.out.println("Wall Time: " + wallTime + " seconds");
      System.out.println("Throughput: " + throughput + " requests per second");

      logRecord();
      StatesCalculator.computeStats(OUTPUT_FILE);
    }

}

class WorkerThread implements Runnable {
  private final String ipAddr;
  private final int apiCalls;

  public WorkerThread(String ipAddr, int apiCalls) {
    this.ipAddr = ipAddr;
    this.apiCalls = apiCalls;
  }

  @Override
  public void run() {
    APIClient client = new APIClient(ipAddr);
    for (int i = 0; i < apiCalls; i++) {
      client.post();
      client.get();
    }
  }
}

class APIClient {
  private final String ipAddr;
  private final HttpClient httpClient;

  public APIClient(String ipAddr) {
    this.ipAddr = ipAddr;
    this.httpClient = HttpClient.newHttpClient();
  }

  public void post() {
    executeRequest("POST");
  }

  public void get() {
    executeRequest("GET");
  }

  private void executeRequest(String method) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ipAddr))
        .method(method, HttpRequest.BodyPublishers.noBody())
        .build();

    long start = System.currentTimeMillis();
    for (int i = 0; i < 5; i++) {
      try {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long end = System.currentTimeMillis();
        long latency = end - start;
        int statusCode = response.statusCode();
        addRecord(start, method, latency, statusCode);

        if (statusCode >= 200 && statusCode < 400) {
          break;
        }

        if (statusCode >= 400 && statusCode < 500) {
          System.err.println("Client error: " + statusCode);
          break;
        }
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
  }

  private void addRecord(long start, String method, long latency, int statusCode) {
    String record = String.format("%s,%s,%d,%d", start, method, latency, statusCode);
    LoadTestClient.globalQueue.add(record);
  }
}

