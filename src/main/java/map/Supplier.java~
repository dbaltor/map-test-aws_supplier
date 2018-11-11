
// Denis test-map event supplier
// server command line: java -jar ./build/distributions/denis-map-test_aws_supplier-1.0-SNAPSHOT/bin/denis-map-test_aws_supplier <fileName. Default: ./files/test.csv> <fullWorkWindow in sec. Default: 120> <sleepInterval in sec. Default: 10>
// example: java -jar ./build/distributions/denis-map-test_aws_supplier-1.0-SNAPSHOT/bin/denis-map-test_aws_supplier ./files/test.csv 120 10

package map;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;


import qm.QueueManager;

public class Supplier {

  private static final String CMD_QUEUE_URL = "https://sqs.eu-west-2.amazonaws.com/556385395922/heatmap-supplier";
  private static final String TOPIC_ARN = "arn:aws:sns:eu-west-2:556385395922:heatmap";
  private static String fileName = ".//files//test.csv";  
  
  private static long fullWorkWindow = 2 * 60 * 1000; // 2 min in millis
  private static int sleepInterval = 10 * 1000; // 10 sec in millis
  
  public static final String QUIT = "quit";
  
  // subclass to supply the events in a different thread
  private static final SupplyTask supplyTask = new SupplyTask();
  
  public static void main(String[] args) {
    
    if (args.length > 0) {
      fileName = args[0];
    }
    if (args.length > 1) {
      fullWorkWindow = Long.valueOf(args[1]);
    }
    if (args.length > 2) {
      sleepInterval = Integer.valueOf(args[2]);
    }    
    System.out.println("Initializing Messaging Supplier with the following configuration...");
    System.out.println("Topic ARN: " + TOPIC_ARN);
    System.out.println("Messages File: " + fileName);
    System.out.println("Work Time Window (ms): " + fullWorkWindow);
    System.out.println("Sleep Interval (ms): " + sleepInterval);
    
    try {
      // load list of heatmap coordinates
      supplyTask.lines = Files.readAllLines(Paths.get(fileName));
      
      // Build the SNS client
      supplyTask.snsClient = SnsClient.builder()
        .region(Region.EU_WEST_2)
        .credentialsProvider(ProfileCredentialsProvider.builder()
                               .profileName("default")
                               .build())
        .build();
      
      while (true) {
        // Poll the queue waiting for a new command to arrive
        List<Message> cmds = QueueManager.get(CMD_QUEUE_URL); 
        if (cmds.isEmpty()) {
//          System.out.println("Idle...");
          continue;
        }
        // some command received
        if (cmds.stream()
//              .peek(msg -> System.out.println(msg.body()))
              .anyMatch(cmd -> cmd.body().equals(QUIT))){
          // command to quit received
          System.out.println("Quiting...");
          System.exit(0);
        }
        
        // Calculate the remaining time to work since the last command was received
        supplyTask.remainingWorkWindow.set(Math.max(0, fullWorkWindow - cmds.stream()
                                              .mapToLong(cmd -> System.currentTimeMillis() -
                                                         Long.valueOf(cmd.attributes().get(MessageSystemAttributeName.SENT_TIMESTAMP)))
//                                            .peek(System.out::println)
                                              .min().getAsLong()));

          supplyTask.run();
        }

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }    
  }
  
  static class SupplyTask {
    public List<String> lines = null;
    public SnsClient snsClient = null;
    public AtomicLong remainingWorkWindow = new AtomicLong(0);
    public AtomicBoolean running = new AtomicBoolean(false);
    
    public void run() {

      // if required, start a separate thread to generate messages for the remainingWorkWindow time 
      if(running.compareAndSet(false,true)){    
        new Thread(() -> {   
          
          while (remainingWorkWindow.get() > 0 && running.get()) { // Test if should carry on
            
            System.out.println("Remaining time for sending messages (ms) = " + remainingWorkWindow.get());
            
            // generate one message containing all lines from the file augmented by a random number between 0 and 2
            String msg = lines.stream()
              .map(line -> line + "," + ThreadLocalRandom.current().nextInt(0,3))
              .collect(Collectors.joining("\n"));
            
            // Publish message to consumers
            PublishResponse response = snsClient.publish(PublishRequest.builder()
                                                           .topicArn(TOPIC_ARN)
                                                           .message(msg)
                                                           .build());
            // Sleeps for the specified INTERVAL
            try{
              Thread.sleep(sleepInterval);
            } catch(InterruptedException ie) {
              System.exit(1);
            }
            remainingWorkWindow.getAndAdd(-sleepInterval);
          }
          running.set(false);
          System.out.println("Work Window has finished...");
        }).start();
      }
    }
  }
}