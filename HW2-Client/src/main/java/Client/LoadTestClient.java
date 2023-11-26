package Client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {

  private static final int INITIAL_THREADS = 10;
  private static final int API_CALLS_PER_THREAD = 1000;

  private static final AtomicLong successfulRequests = new AtomicLong(0);
  private static final AtomicLong failedRequests = new AtomicLong(0);
  private static BufferedWriter writer;
  private static final String OUTPUT_FILE = "output.csv";
  private static Queue<String> globalQueue = new ConcurrentLinkedQueue<>();

  private static void logRecord() throws IOException {
    writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true));
    for(String record:globalQueue) {
      try {
        writer.write(record);
        writer.flush();
      } catch (IOException e) {
        System.err.println("Failed to log request: " + e.getMessage());
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.out.println("Usage: Client.LoadTestClient <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
      return;
    }

    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String ipAddr = args[3];

    // Initialization Phase
    ExecutorService initService = Executors.newFixedThreadPool(INITIAL_THREADS);
    for (int i = 0; i < INITIAL_THREADS; i++) {
      initService.submit(new WorkerThread(ipAddr, 100,globalQueue,successfulRequests,failedRequests));
    }
    initService.shutdown();
    initService.awaitTermination(1, TimeUnit.HOURS);

    System.out.println("Initialization Phase finished, The Url is "+ ipAddr);

    long startTime = System.currentTimeMillis();

    // Test Phase
    ExecutorService mainService = Executors.newFixedThreadPool(threadGroupSize * numThreadGroups);
    for (int i = 0; i < numThreadGroups; i++) {
      for (int j = 0; j < threadGroupSize; j++) {
        mainService.submit(new WorkerThread(ipAddr, API_CALLS_PER_THREAD,globalQueue,successfulRequests,failedRequests));
      }
      if (i < numThreadGroups - 1) { // don't delay after the last group
        Thread.sleep(delay * 1000L);
      }
    }
    mainService.shutdown();
    mainService.awaitTermination(1, TimeUnit.HOURS);

    long endTime = System.currentTimeMillis();
    double wallTime = (endTime-startTime) / 1000;
    long totalRequests = (long) threadGroupSize * numThreadGroups * API_CALLS_PER_THREAD;
    double throughput = totalRequests / wallTime;

    System.out.println("Wall Time: " + wallTime + " seconds");
    System.out.println("Throughput: " + throughput + " requests per second");
    logRecord();
    StatesCalculator.computeStats("output.csv");
  }
}

class WorkerThread implements Runnable {
  private final String ipAddr;
  private final int apiCalls;
  private final APIClient client;

  public WorkerThread(String ipAddr, int apiCalls, Queue<String> que,AtomicLong success,AtomicLong fail) throws IOException {
    this.ipAddr = ipAddr;
    this.apiCalls = apiCalls;
    this.client = new APIClient(ipAddr,que,success,fail);
  }

  @Override
  public void run() {
    for (int i = 0; i < apiCalls; i++) {
      client.post();
      client.get();
    }
  }
}

class APIClient {
  private final String ipAddr;
  private final HttpClient httpClient;
  private byte[] imageBytes;
  private Queue<String> records;

  private final AtomicLong successfulRequests;
  private final AtomicLong failedRequests;
  public APIClient(String ipAddr,Queue<String> queue,AtomicLong successfulRequests, AtomicLong failedRequests) throws IOException {
    this.ipAddr = ipAddr;
    this.httpClient = HttpClient.newHttpClient();
    this.records = queue;
    this.successfulRequests = successfulRequests;
    this.failedRequests = failedRequests;
  }

  public void post() {
    String boundary = UUID.randomUUID().toString();
    String jsonString = "{\n" +
        "    \"artist\": \"KAI ZI\",\n" +
        "    \"title\": \"What's up!\",\n" +
        "    \"year\": \"1987\"\n" +
        "}";

    String albumDataPart = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"albumData\"\r\n\r\n"
        + jsonString + "\r\n";

    String imageHeader = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"image\"; filename=\"filename.jpg\"\r\n"
        + "Content-Type: image/jpeg\r\n\r\n";
    if(imageBytes == null) {
      try {
        imageBytes = Files.readAllBytes(Path.of("/Users/kaixi/Desktop/File/NEU/CS6650/HW1_Kai_Xi/HW2-Client/Xi.jpeg"));
      } catch (IOException e) {
        throw new RuntimeException("Failed to read image file", e);
      }
    }

    String endBoundary = "\r\n--" + boundary + "--\r\n";

    List<byte[]> byteArrays = Arrays.asList(albumDataPart.getBytes(), imageHeader.getBytes(), imageBytes, endBoundary.getBytes());

    HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ipAddr))
        .POST(publisher)
        .header("Content-Type", "multipart/form-data;boundary=" + boundary)  // Set the header here
        .build();

    executeHttpRequest(request,1);
  }

  public void get() {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ipAddr))
        .GET()
        .build();
    executeHttpRequest(request,0);
  }

  private void executeHttpRequest(HttpRequest request,int method) {
    long start = System.currentTimeMillis();
    for (int i = 0; i < 5; i++) {
      try {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        long end = System.currentTimeMillis();
        long latency = end - start;
        addRecord(start,method,latency,statusCode);
        if (statusCode >= 200 && statusCode < 400) {
          successfulRequests.incrementAndGet();
          break; // successful request, break out of retry loop
        }
        if (statusCode >= 400 && statusCode <= 500) {
          failedRequests.incrementAndGet();
          System.err.println("Client error: " + statusCode);
          break; // client error, break out of retry loop
        }
        // If it's a 5XX error, it'll continue to the next iteration to retry
      } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
  }

  private void addRecord(long start, int method, long latency, int statusCode) {
    String record = String.format("%s,%d,%d,%d\n", start, method, latency, statusCode);
    this.records.add(record);
  }
}