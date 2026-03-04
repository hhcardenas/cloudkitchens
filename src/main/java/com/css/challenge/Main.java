package com.css.challenge;

import com.css.challenge.client.Client;
import com.css.challenge.client.Order;
import com.css.challenge.client.Problem;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "challenge", showDefaultValues = true)
public class Main implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  static {
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT: %5$s %n");
  }

  @Option(names = "--endpoint", description = "Problem server endpoint")
  String endpoint = "https://api.cloudkitchens.com";

  @Option(names = "--auth", description = "Authentication token (required)")
  String auth = "";

  @Option(names = "--name", description = "Problem name. Leave blank (optional)")
  String name = "";

  @Option(names = "--seed", description = "Problem seed (random if zero)")
  long seed = 0;

  @Option(names = "--rate", description = "Inverse order rate")
  Duration rate = Duration.ofMillis(500);

  @Option(names = "--min", description = "Minimum pickup time")
  Duration min = Duration.ofSeconds(4);

  @Option(names = "--max", description = "Maximum pickup time")
  Duration max = Duration.ofSeconds(8);

  @Option(names = "--coolers", description = "Number of cooler storage spaces")
  int coolerCapacity = 6;

  @Option(names = "--heaters", description = "Number of heater storage spaces")
  int heaterCapacity = 6;

  @Option(names = "--shelves", description = "Number of shelf storage spaces")
  int shelfCapacity = 12;

  @Override
  public void run() {
    try {
      Client client = new Client(endpoint, auth);
      Problem problem = client.newProblem(name, seed);
      List<Order> orders = problem.getOrders();

      LOGGER.info("Received {} orders", orders.size());

      // ------ Execution harness logic goes here using rate, min and max ----

      Kitchen kitchen = new Kitchen(coolerCapacity, heaterCapacity, shelfCapacity);
      ScheduledExecutorService pickupExecutor = Executors.newScheduledThreadPool(12);
      ThreadLocalRandom random = ThreadLocalRandom.current();
      CountDownLatch allPickups = new CountDownLatch(orders.size());

      // Place orders one by one at the configured rate.
      // After each placement, schedule a pickup at a random delay within [min, max].
      for (Order order : orders) {
        kitchen.placeOrder(order);

        long delayMs = random.nextLong(min.toMillis(), max.toMillis());
        pickupExecutor.schedule(() -> {
          try {
            kitchen.pickup(order.getId());
          } finally {
            allPickups.countDown();
          }
        }, delayMs, TimeUnit.MILLISECONDS);

        Thread.sleep(rate.toMillis());
      }

      // Wait for all scheduled pickups to complete
      allPickups.await();
      pickupExecutor.shutdown();

      // ----------------------------------------------------------------------

      String result = client.solveProblem(problem.getTestId(), rate, min, max, kitchen.get_actions());
      LOGGER.info("Result: {}", result);
      System.out.println(kitchen.get_actions());

    } catch (IOException | InterruptedException e) {
      LOGGER.error("Simulation failed: {}", e.getMessage());
    }
  }

  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }
}
