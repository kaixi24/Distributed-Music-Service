package Client;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.csv.CSVFormat;
public class StatesCalculator {
  public static void computeStats(String filePath) throws Exception {
    // Create a CSV parser
    try (Reader in = new FileReader(filePath)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

      List<Integer> latencies1 = new ArrayList<>();
      List<Integer> latencies2 = new ArrayList<>();
      // Extract latency values from the CSV and add to the list
      for (CSVRecord record : records) {
        Integer method = Integer.parseInt(record.get(1)); // assuming method is the second column, 0 mean get, 1 means post
        Integer latency = Integer.parseInt(record.get(2)); // assuming latency is the third column
        if(method == 0) {
          latencies1.add(latency);
        } else {
          latencies2.add(latency);
        }
      }

      // Convert list to array for DescriptiveStatistics
      double[] getlatencyArray = latencies1.stream().mapToDouble(d -> d).toArray();
      double[] postlatencyArray = latencies2.stream().mapToDouble(d -> d).toArray();
      // Compute statistics
      DescriptiveStatistics getstats = new DescriptiveStatistics(getlatencyArray);
      DescriptiveStatistics poststats = new DescriptiveStatistics( postlatencyArray );
      // Output results
      System.out.println("Get Statistics");
      System.out.println("Mean: " + getstats.getMean() + " ms");
      System.out.println("Median: " + getstats.getPercentile(50) + " ms");
      System.out.println("99th Percentile: " + getstats.getPercentile(99) + " ms");
      System.out.println("Min: " + getstats.getMin() + " ms");
      System.out.println("Max: " + getstats.getMax() + " ms\n");
      System.out.println("Post Statistics");
      System.out.println("Mean: " + poststats .getMean() + " ms");
      System.out.println("Median: " +poststats .getPercentile(50) + " ms");
      System.out.println("99th Percentile: " + poststats .getPercentile(99) + " ms");
      System.out.println("Min: " + poststats .getMin() + " ms");
      System.out.println("Max: " + poststats .getMax() + " ms");
    }
  }
}
