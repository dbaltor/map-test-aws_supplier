package qm;

//import software.amazon.awssdk.core.auth.policy.Policy;
/*import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.services.iot.model.SqsAction;*/
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/************************************************************************\
 * Utility class to interact with AWS Simple Queue Service (SQS)
 * 
 * Author: Denis Baltor
\************************************************************************/
public class QueueManager {
  
  private static final String TEST_QUEUE_NAME = "test-queue-" + System.currentTimeMillis();
  private static final String DLQ_ARN = "arn:aws:sqs:eu-west-2:556385395922:heatmap-dlq";

  /**
   * The class runs a set of integration tests when executed standalone.
   * 1) Create a test queue
   * 2) Put a test message on the test queue
   * 3) Get the test message from the test queue
   * 4) Delete the test queue
   */
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
   
    System.out.println("Testing QueueManager...");
    System.out.println("Using Dead Letter Queue: " + DLQ_ARN);
    System.out.println("==============================================================");
    System.out.println("Creating a new queue: " + TEST_QUEUE_NAME);
    
    String testQueueURL = "";
    try{
      testQueueURL = createQueue(TEST_QUEUE_NAME, DLQ_ARN);
      System.out.println("Created test queue with URL: " + testQueueURL);
    }catch( Exception e) {
      System.out.println("Exception caught while trying creating the test queue...");
      e.printStackTrace();
      System.exit(1);
    }
    
    System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
    sc.nextLine();
 
 
    try{
      System.out.println("ARN of the newly created queue: " + getQueueARN(testQueueURL));
    }catch(Exception e) {
      System.out.println("Exception caught while trying enquiring the queue's ARN...");
      e.printStackTrace();
      System.exit(1);
    }

    System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
    sc.nextLine();
    
    System.out.println("Testing SYNCHRONOUS API...");
    System.out.println("==========================");
 
    String test_msg = "Test message number: " + System.currentTimeMillis();

    System.out.println("Using queue: " + testQueueURL);
    System.out.println("The following message will be written into the test queue...");
    System.out.println(test_msg);
    
    try {
      put(testQueueURL, test_msg);
      System.out.println("Done!");
    }catch( Exception e) {
      System.out.println("Exception caught while trying writing into the test queue...");
      e.printStackTrace();
      System.exit(1);
    }
    
    System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
    sc.nextLine();
    
    System.out.println("The following messages will be read from the test queue...");
    get(testQueueURL) 
      .stream()
      .map(msg -> msg.body())
      .forEach(System.out::println);
    
    System.out.println("Done!");
    System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
    sc.nextLine();
    
    System.out.println();
    System.out.println("Testing ASYNCHRONOUS API...");
    System.out.println("===========================");    
    
    test_msg = "Test message number: " + System.currentTimeMillis();

    System.out.println("Using queue: " + testQueueURL);
    System.out.println("The following message will be written into the test queue...");
    System.out.println(test_msg);
    
    try {
      put(testQueueURL, test_msg);
      System.out.println("Done!");
    }catch( Exception e) {
      System.out.println("Exception caught while trying writing into the test queue...");
      e.printStackTrace();
      System.exit(1);
    }

    System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
    sc.nextLine();
    
    System.out.println("The following messages will be ASYNCHRONOUSLY read from the test queue...");

    final String TEST_QUEUE_URL = testQueueURL;
    getAsync(TEST_QUEUE_URL).whenComplete((msgs, err) -> {
      if (msgs != null) {
        msgs.stream()
          .map(msg -> msg.body())
          .forEach(System.out::println);
        
        System.out.println("Done!");
        System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
        sc.nextLine();    
      } else {
        // Handle error
        err.printStackTrace();
        System.exit(1);
      }
    }).join(); // wait for the future to complete
    
    try{
      System.out.println("Will delete the new queue: " + TEST_QUEUE_NAME);
      System.out.println("Thread " + Thread.currentThread().getName() + ": Press ENTER...");
      sc.nextLine();
      
      deleteQueue(testQueueURL);
      System.out.println("Done!");      
    }catch( Exception e) {
      System.out.println("Exception caught while trying deleting the test queue...");
      e.printStackTrace();
      System.exit(1);
    }
    
    System.out.println();
    System.out.println("*******************");
    System.out.println("ALL TESTS PASSED!!!");
    System.out.println("*******************");    
  }
  
  /**
   * Build a synchronous SQS client
   * 
   * @return the client built
  */
  private static SqsClient buildClient() {
    // Build a SQS service client
    return SqsClient
      .builder()
//      .region(Region.EU_WEST_2)
      .build();      
  }
  
  /**
   * Build an asynchronous SQS client
   * 
   * @return the client built
  */
  private static SqsAsyncClient buildAsyncClient() {
    // Build a SQS service client
    return SqsAsyncClient
      .builder()
      .build();      
  }  
  
  /**
   * Create a new queue
   * 
   * @param queueName Name of the queue to be created
   * @return The new queue's URL
   * @throws Exception
   */    
  public static String createQueue(String queueName) throws Exception{
    return buildClient()
      .createQueue(CreateQueueRequest
                     .builder()
                     .queueName(queueName)
                     .build())
      .queueUrl();  
  }
  
  /**
   * Create a new queue with an associated dead-letter queue
   * 
   * @param queueName Name of the queue to be created
   * @param deadLetterQueueARN Dead-letter queue's ARN to be used
   * @return The new queue's URL
   * @throws Exception
   */   
  public static String createQueue(String queueName, String deadLetterQueueARN) throws Exception{
    return buildClient()
      .createQueue(CreateQueueRequest
                     .builder()
                     .queueName(queueName)
                     .attributes(new HashMap<QueueAttributeName, String>(){{
      put(QueueAttributeName.REDRIVE_POLICY, 
          "{\"maxReceiveCount\":\"5\", " + 
          "\"deadLetterTargetArn\":\"" + 
          deadLetterQueueARN + "\"}");}})
                     .build())
      .queueUrl();  
  }

  /**
   * Create a new queue with associated dead-letter queue and pub/sub topic
   * 
   * @param queueName Name of the queue to be created
   * @param deadLetterQueueARN Dead-letter queue's ARN to be used
   * @param topicARN
   * @return The new queue's URL
   * @throws Exception
   */   
  public static String createQueue(String queueName, String deadLetterQueueARN, String topicARN) throws Exception
  {
    SqsClient sqsClient = buildClient();
    String queueUrl = sqsClient
      .createQueue(CreateQueueRequest
                     .builder()
                     .queueName(queueName)
                     .build())
      .queueUrl();
    final String queueARN = sqsClient    
      .getQueueAttributes(GetQueueAttributesRequest
                            .builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build())
      .attributes().get(QueueAttributeName.QUEUE_ARN);
    sqsClient.setQueueAttributes(
                                 SetQueueAttributesRequest
                                   .builder()
                                   .queueUrl(queueUrl)
                                   .attributes(
                                               new HashMap<QueueAttributeName, String>(){{
      put(QueueAttributeName.REDRIVE_POLICY, 
          "{\"maxReceiveCount\":\"5\", " + 
          "\"deadLetterTargetArn\":\"" + 
          deadLetterQueueARN + "\"}");
      put(QueueAttributeName.POLICY,
          "{\"Statement\": [{" +
          "\"Effect\": \"Allow\", " +
          "\"Principal\": {\"AWS\": \"*\"}," +
          "\"Action\": \"SQS:SendMessage\", " +
          "\"Resource\": \"" + queueARN + "\"," +
          "\"Condition\": {" +
          "\"ArnEquals\": {\"aws:SourceArn\": \"" + topicARN + "\"}" +
          "}" +
          "}]}");}})
                                   .build());
    return queueUrl;
  }  
  
  /**
   * Get the queue's ARN
   * 
   * @param queueUrl URL of the queue to enquire
   * @return The queue's ARN
   * @throws Exception
   */ 
  public static String getQueueARN(String queueUrl) throws Exception
  {
    return buildClient()
      .getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build())
      .attributes().get(QueueAttributeName.QUEUE_ARN);
  }

  /**
   * Delete the queue provided
   * 
   * @param queueUrl URL of the queue to be deleted
   * @throws Exception
   */     
  public static void deleteQueue(String queueUrl) throws Exception{
    buildClient()
      .deleteQueue(
                   DeleteQueueRequest.builder()
                     .queueUrl(queueUrl)
                     .build());
  }
  
  /**
   * Put the given message into the queue provided
   * 
   * @param queueUrl Target queue's URL
   * @param messageBody Content to be sent
   * @return The message id generated
   * @throws Exception
   */  
  public static String put(String queueUrl, String messageBody) throws Exception{
    return buildClient()
      .sendMessage(SendMessageRequest.builder()
                     .queueUrl(queueUrl)
                     .messageBody(messageBody)
                     .build())
      .messageId();    
  }
 
  /**
   * Put the given message into the queue provided
   * 
   * @param queueUrl Target queue's URL
   * @param messageBody Content to be sent
   * @param delaySeconds Time to wait before sending the message
   * @return The message id generated
   * @throws Exception
   */   
  public static String put(String queueUrl, String messageBody, Integer delaySeconds) throws Exception{ 
    return buildClient()
      .sendMessage(SendMessageRequest.builder()
                     .queueUrl(queueUrl)
                     .messageBody(messageBody)
                     .delaySeconds(delaySeconds)
                     .build())
      .messageId();
  }
  
  /**
   * Retrieve up to 5 messages from the queue provided.
   * The queue will be polled up to 20 seconds.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @return The list of messages retrieved
   */   
  public static List<Message> get(String queueUrl) {
    // long poll the queue for 20 seconds to read up to 5 messages
    return get(queueUrl, 5, 20);    
  }
  
  /**
   * Retrieve up to the specified number of messages from the queue provided.
   * The queue will be polled up to 20 seconds.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @param maxNumberOfMessages maximum number of messages to be retrieved
   * @return The list of messages retrieved
   */   
  public static List<Message> get(String queueUrl, int maxNumberOfMessages) {
    // long poll the queue for 20 seconds to read up to <maxNumberOfMessages> messages
    return get(queueUrl, maxNumberOfMessages, 20);
  }
  
  /**
   * Retrieve up to the specified number of messages from the queue provided
   * The queue will be polled up to the specified amount of time.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @param maxNumberOfMessages maximum number of messages to be retrieved
   * @param waitTimeSeconds maximum number of seconds to wait
   * @return The list of messages retrieved
   */  
  public static List<Message> get(String queueUrl, int maxNumberOfMessages, int waitTimeSeconds) {
    SqsClient sqsClient = buildClient();
    
    // long poll the queue for <waitTimeSeconds> seconds to read up to <maxNumberOfMessages> messages
    List<Message> messages = sqsClient.receiveMessage(
                                                      ReceiveMessageRequest
                                                        .builder()
                                                        .queueUrl(queueUrl)
                                                        .maxNumberOfMessages(maxNumberOfMessages)
                                                        .waitTimeSeconds(waitTimeSeconds)
                                                        .attributeNames(QueueAttributeName.ALL)
                                                        .build())
      .messages();

    // delete from the queue all messages read
    for (Message message : messages) {
      sqsClient.deleteMessage(
                              DeleteMessageRequest
                                .builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());
    }
    
    return messages;
  }
  
  /**
   * Asynchronously retrieve up to 5 messages from the queue provided.
   * The queue will be polled up to 20 seconds.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @return A completable future of the list of messages to be retrieved
   */   
  public static CompletableFuture<List<Message>> getAsync(String queueUrl) {
    // long poll the queue for 20 seconds to read up to 5 messages
    return getAsync(queueUrl, 5, 20);    
  }
  
  /**
   * Asynchronously retrieve up to the specified number of messages from the queue provided.
   * The queue will be polled up to 20 seconds.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @param maxNumberOfMessages maximum number of messages to be retrieved
   * @return A completable future of the list of messages to be retrieved
   */   
  public static CompletableFuture<List<Message>> getAsync(String queueUrl, int maxNumberOfMessages) {
    // long poll the queue for 20 seconds to read up to <maxNumberOfMessages> messages
    return getAsync(queueUrl, maxNumberOfMessages, 20);
  }  
  
  /**
   * Asynchronously retrieve up to the specified number of messages from the queue provided
   * The queue will be polled up to the specified amount of time.
   * All message attributes will be retrieved.
   * 
   * @param queueUrl Target queue's URL
   * @param maxNumberOfMessages maximum number of messages to be retrieved
   * @param waitTimeSeconds maximum number of seconds to wait
   * @return A completable future of the list of messages to be retrieved
   */  
  public static CompletableFuture<List<Message>> getAsync(String queueUrl, int maxNumberOfMessages, int waitTimeSeconds) {
    // long poll the queue for <waitTimeSeconds> seconds to read up to <maxNumberOfMessages> messages
    CompletableFuture<ReceiveMessageResponse> response = buildAsyncClient()
      .receiveMessage(
                      ReceiveMessageRequest
                        .builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxNumberOfMessages)
                        .waitTimeSeconds(waitTimeSeconds)
                        .attributeNames(QueueAttributeName.ALL)
                        .build());
    
    CompletableFuture<List<Message>> messages = response.thenApply(ReceiveMessageResponse::messages);

    // delete from the queue all messages read
    SqsClient sqsClient = buildClient();
    messages.thenAccept(msgs -> {
      for (Message message : msgs) {
        sqsClient.deleteMessage(
                                DeleteMessageRequest
                                  .builder()
                                  .queueUrl(queueUrl)
                                  .receiptHandle(message.receiptHandle())
                                  .build());
      }
    });
    
    return messages;
  }  
}