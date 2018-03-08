package com.tierconnect.riot.commons.actions.executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Charsets;
import com.tierconnect.riot.commons.actions.connections.GooglePubSubConnection;
import com.tierconnect.riot.commons.actions.messages.GooglePubSubMessage;
import com.tierconnect.riot.commons.actions.ruleconfigs.GooglePubSubConfig;
import com.tierconnect.riot.commons.dtos.ActionMessageDto;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Publish a message to Google pub/sub
 * Created by vramos on 07-07-17.
 */
public class GooglePubSubExecutor extends OutputActionExecutor{
    private Logger logger = Logger.getLogger( GooglePubSubExecutor.class );
    private ObjectMapper mapper = new ObjectMapper();
    private SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private JacksonFactory jsonFactory;
    private NetHttpTransport transport;
    private GoogleCredential credential;
    private HttpRequestFactory requestFactory;
    private GooglePubSubConnection gConnection;
    private Map<String,Object> properties;
    private BlockingDeque<GooglePubSubMessage> messagesQueue;
    private HttpPublisher publisher;
    private Base64.Encoder encoder = Base64.getEncoder();
    private int maxQueueSize;

    /**
     * Initializes the google pub/sub connection and a thread pool.
     * @param actionMessage
     * @throws Exception
     */
    public GooglePubSubExecutor(ActionMessageDto actionMessage, Map<String,Object> properties) throws Exception{
        logger.info(String.format(
                "ActionExecutor: Initializing Google Pub/Sub Executor\n\tconnection: %s\n\tproperties: %s",
                mapper.writeValueAsString(actionMessage.connection),
                mapper.writeValueAsString(properties)));

        this.properties = properties;
        this.maxQueueSize = Integer.parseInt(properties.getOrDefault("maxQueueSize",500).toString());
        messagesQueue = new LinkedBlockingDeque<>(maxQueueSize);
        gConnection = (GooglePubSubConnection) actionMessage.connection;
        jsonFactory = JacksonFactory.getDefaultInstance();
        transport = GoogleNetHttpTransport.newTrustedTransport();
        credential = GoogleCredential.fromStream(
                new ByteArrayInputStream(mapper.writeValueAsBytes(gConnection)),
                transport,
                jsonFactory);
        Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/pubsub");
        credential = credential.createScoped(scopes);
        requestFactory = transport.createRequestFactory();
        GooglePubSubConfig gConfig = (GooglePubSubConfig) actionMessage.configuration;
        GenericUrl url = new GenericUrl(String.format("https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish", gConnection.projectId, gConfig.topic));
        publisher = new HttpPublisher("GooglePubSubThread-"+gConfig.topic, url,requestFactory,credential);
        publisher.start();
    }

    /**
     * Generates the request message and delegates its execution to the thread pool.
     * @throws Exception all action must throws an exception in order to be handled by the ActionExecutor
     */
    public void execute(Object config) throws Exception{
        GooglePubSubConfig gConfig = (GooglePubSubConfig) config;
        GooglePubSubMessage message = new GooglePubSubMessage();
        String strData = mapper.writeValueAsString(gConfig.data);
        message.data = new String(encoder.encode(strData.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
        message.attributes = gConfig.attributes;
        message.publishTime = dateFormater.format(new Date());
        message.messageId = GooglePubSubConfig.class.getSimpleName() + UUID.randomUUID().toString();
        messagesQueue.put(message); // don't use offer or add

    }

    /**
     * Builds a sends one set of messages to a google pub/sub service:
     * e.g.
     * {
     *      "data": "11C651B51CA61B65C51D651651E65151FE5D616D165C1651B616A516A65E16D5E",
     *      "attributes": {
     *          "epc": "000000000000000000001",
     *          "zone_entered": "Po1"
     *      },
     *      "messageId": "111515705419672",
     *      "publishTime": "2017-06-05 15:22:01.689Z"
     * }
     *
     * Also, implements a retry policy with a exponential backoff strategy.
     *
     * Needs a clean up or improvement !
     */
    class HttpPublisher extends Thread {
        private GoogleCredential credential;
        private HttpRequestFactory requestFactory;
        private GenericUrl url;

        private int maxRetryIntervalSeconds;
        private int failAfterTimeoutMinutes;
        private int requestIntervalMs;
        private final int[] retryableCodes = new int[]{499,504,500,429,503};

        private long sleepDuration = 0;

        public HttpPublisher(String threadName, GenericUrl url, HttpRequestFactory requestFactory, GoogleCredential credential){
            super(threadName);
            this.url = url;
            this.requestFactory = requestFactory;
            this.credential = credential;
            this.maxRetryIntervalSeconds = Integer.parseInt(properties.getOrDefault("maxRetryIntervalSeconds",60).toString());
            this.failAfterTimeoutMinutes = Integer.parseInt(properties.getOrDefault("failAfterTimeoutMinutes",1440).toString());
            this.requestIntervalMs = Integer.parseInt(properties.getOrDefault("requestIntervalMs",200).toString());

        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(requestIntervalMs);
                } catch (InterruptedException e) { }

                if ( ! messagesQueue.isEmpty() ) {
                    long a = System.currentTimeMillis();
                    boolean completed = false;
                    boolean failed = false;
                    int retryCount = 1;
                    List<GooglePubSubMessage> messages = new ArrayList<>();

                    while( !messagesQueue.isEmpty() && messages.size() < maxQueueSize){
                        messages.add(messagesQueue.poll());
                    }

                    do {
                        try {
                            logger.info("Executing post request to: " + url);
                            String msg = mapper.writeValueAsString(messages);
                            msg = "{\"messages\":"+msg+"}";
                            HttpRequest request = requestFactory.buildPostRequest(url,
                                    ByteArrayContent.fromString("application/json", msg));
                            String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
                            logger.info("Request Message: "+indented);
                            logger.info("Google pub/sub messages count=" + messages.size());
                            credential.initialize(request);
                            HttpResponse response = request.execute();
                            if (response.getStatusCode() == 200) {
                                completed = true;
                                logger.info(String.format("Publish to GoogleCloud Pub/Sub was successful. Got response %d - %s", response.getStatusCode(), IOUtils.toString(response.getContent())));
                                logger.info(String.format("\nMessage sent to Google. \nEndpoint: %s\nGoogle Response code: 200", url));
                            } else {
                                logger.error(String.format("Publish to GoogleCloud Pub/Sub returns code=%d message=%s", response.getStatusCode(), response.getStatusMessage()));
                            }
                        } catch (IOException e) {
                            if(e instanceof HttpResponseException){
                                HttpResponseException ex = (HttpResponseException)e;
                                if (IntStream.of(retryableCodes).anyMatch(x -> x == ex.getStatusCode())) {
                                    logger.error(String.format("Publish to GoogleCloud Pub/Sub returns code=%d message=%s", ex.getStatusCode(), ex.getMessage()), ex);
                                    failed = sleepExponentialBackoff(retryCount);
                                    retryCount++;
                                } else{
                                    logger.error(String.format("Publish to GoogleCloud Pub/Sub returns code=%d message=%s", ex.getStatusCode(), ex.getStatusMessage()), ex);
                                    failed = true;
                                }
                            } else if(e instanceof SocketException
                                    || e instanceof SocketTimeoutException
                                    || e instanceof UnknownHostException
                                    || e instanceof ConnectException
                                    || e instanceof InterruptedIOException
                                    || e instanceof NoRouteToHostException){
                                logger.error("A network error occurred while sending request to Google PubSub service.", e);
                                failed = sleepExponentialBackoff(retryCount);
                                retryCount++;
                            } else{
                                logger.error("An error occurred while sending request to Google PubSub service.", e);
                                failed = true;
                            }
                        }
                    } while (!completed && !failed);

                    if(!completed){
                        logger.error(String.format("\nGOOGLE PUB/SUB EXECUTOR FAILURE: Could not send a message. attempts=%d",(retryCount-1)));
                    } else{
                        long b = System.currentTimeMillis();
                            logger.info(String.format("\nGOOGLE PUB/SUB EXECUTOR SUCCESS: %d ms attempts=%d", (b-a), retryCount));
                    }
                }
            }
        }

        /**
         * Sleeps applying a exponential backoff
         * @return true if the sleep duration has exceeded the fail timeout
         */
        private boolean sleepExponentialBackoff(int retryCount){
            int sleepSeconds = 1 << retryCount;
            if(sleepSeconds > maxRetryIntervalSeconds){
                sleepSeconds = maxRetryIntervalSeconds;
            }
            sleepDuration += sleepSeconds;
            logger.info( String.format("Retrying in %d seconds",sleepSeconds) );
            try {
                Thread.sleep( sleepSeconds*1000 );
            } catch (InterruptedException e) { }

            return sleepDuration >= failAfterTimeoutMinutes*60;
        }

    }

}
