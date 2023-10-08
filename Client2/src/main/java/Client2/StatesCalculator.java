package Client2;

import java.io.FileReader;
import java.io.Reader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class StatesCalculator {

  public static void computeStats(String filePath) {
    DescriptiveStatistics stats = new DescriptiveStatistics();

    try (Reader in = new FileReader(filePath)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

      // Extract latency values from the CSV and directly add to the DescriptiveStatistics object
      for (CSVRecord record : records) {
        if (record.size() > 2) { // Ensure the record has enough columns
          try {
            double latency = Double.parseDouble(record.get(2)); // Assuming latency is the third column
            stats.addValue(latency);
          } catch (NumberFormatException e) {
            System.err.println("Invalid latency value in record: " + record.toString());
          }
        }
      }

      // Output results
      System.out.println("Mean: " + stats.getMean() + " ms");
      System.out.println("Median: " + stats.getPercentile(50) + " ms");
      System.out.println("99th Percentile: " + stats.getPercentile(99) + " ms");
      System.out.println("Min: " + stats.getMin() + " ms");
      System.out.println("Max: 1" + stats.getMax() + " ms");

    } catch (Exception e) {
      System.err.println("Error processing the CSV file: " + e.getMessage());
    }
  }
}
