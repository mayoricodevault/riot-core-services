package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.EmailSender;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.services.broker.BrokerConnection;
import com.tierconnect.riot.commons.services.broker.MQTTEdgeConnectionPool;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.*;

/**
 * This class is used to listen for request of notifications from mqtt
 * @author bbustillos on 10/5/2015.
 */
public class NotificationService implements MqttCallback{

    private static final Logger logger = Logger.getLogger( NotificationService.class );

    private static List<NotificationService> services = new ArrayList<>();
    private EmailSender.SmtpParameters mailParameters;
    private EmailSender.EmailMessageParameters messageParameters;
    MqttClient client;
    Thread t;
    int qos = 2;

    public static void main( String[] args ) {
        init( "notification-"+ UUID.randomUUID().toString());
    }

    public static void init( String clientId ) {
        Map<String, BrokerConnection> mapConnections = MQTTEdgeConnectionPool.getInstance().getAllLstConnections();
        if ( !mapConnections.isEmpty() ){
            StringBuilder sb = new StringBuilder();
            sb.append("NotificationService: Connections loaded:\n");
            for (Map.Entry<String, BrokerConnection> map : mapConnections.entrySet()){
                BrokerConnection connection = map.getValue();
                NotificationService service = new NotificationService();
                service.connect(
                        connection.getProperties().get("host").toString(),
                        Integer.parseInt(connection.getProperties().get("port").toString()),
                        Integer.parseInt(connection.getProperties().get("qos").toString()),
                        "NS-" +  connection.getCode()+ "-" + clientId,
                        connection.getProperties().containsKey("username")?
                                connection.getProperties().get("username").toString():null,
                        connection.getPassword());
                service.start();
                service.qos = Integer.parseInt(connection.getProperties().get("qos").toString());
                services.add(service);
                sb.append(connection.toString()+"\n");
            }
            logger.info(sb.toString());
        } else {
            logger.warn("No initialize NotificationService. Verify the following causes:\n " +
                    "1. There is no a bridge configuration (type:core or Rules_processor) registered.\n" +
                    "2. Some  bridge configuration (type:core or Rules_processor) does not have a mqtt connection assigned.");
        }
    }

    public static List<NotificationService> getInstances()
    {
        return services;
    }

    public NotificationService()
    {

    }

    private void start() {
        t = new Thread()
        {
            public void run()
            {
                boolean run = true;

                while( run )
                {
                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch( InterruptedException e )
                    {
                        run = false;
                    }

                    if( run && client!=null && !client.isConnected() )
                    {
                        logger.info( "client is not connected, attempting reconnection to: "+client.getServerURI() );
                        try
                        {
                            client.connect();
                            logger.info( "reconnect succeeded ! "+client.getServerURI() );
                            subscribe(qos);
                            logger.info( "subscribe succeeded ! "+client.getServerURI() );
                        }
                        catch( MqttException e )
                        {
                            logger.warn( "Connection Lost, reconnecting to " + client.getServerURI() + " MQTT server "+ e.getMessage());
                            try
                            {
                                Thread.sleep( 5000 );
                            }
                            catch( InterruptedException e2 )
                            {
                                run = false;
                            }
                        }
                    }
                }
                logger.info( "notification thread ended" );
            };
        };
        t.start();
        logger.info("notification thread started");
    }

    private int connect( String mqtthost, int mqttport, int qos, String clientId, String mqttUsername, String mqttPassword ) {
        try
        {
            String url = "tcp://" + mqtthost + ":" + mqttport;
            client = new MqttClient( url, clientId );

            MqttConnectOptions co = new MqttConnectOptions();
            if (mqttUsername != null && !mqttUsername.isEmpty() && mqttPassword != null && !mqttPassword.isEmpty()) {
                co.setUserName(mqttUsername);
                co.setPassword(mqttPassword.toCharArray());
            }

            client.connect(co);
            logger.info( String.format( "Connected to mqtt broker: url=%s clientId=%s", url, clientId ) );

            subscribe(qos);
        }
        catch( MqttException e )
        {
            logger.info( "caught exception: " + e );
            return 1;
        }
        return 0;
    }

    private void subscribe(int qos) throws MqttException
    {
        String topicSubscription = "/v1/notification/#";
        client.subscribe( topicSubscription, qos );
        logger.info( "subscribed to topic=" + topicSubscription + " with qos=" + qos );

        client.setCallback( this );
        logger.info( "callback set" );
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived( String topic, MqttMessage mqttm ) throws Exception {
        Transaction transaction = null;
        try
        {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            transaction = session.getTransaction();
            transaction.begin();
            logger.debug( "topic='" + topic + "'" );
            String body = new String( mqttm.getPayload(), "UTF-8" );
            String[] lines = body.split( "\n" );

            for( int i = 0; i < lines.length; i++ )
            {
                parse( lines[i] );
                logger.info("lines[i] >>> " + lines[i]);
            }
            logger.info( "Email successfully sent to " + lines.length + " recipients.");
            transaction.commit();
        }
        catch( Exception e )
        {
            logger.warn( "Exception parsing message body: ", e);
            HibernateDAOUtils.rollback(transaction);
        }
    }

    // It is necessary to change the parsing to new topic for notifications
    private void parse( String line )
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map;
        Map<String, Object> mapBody;

        /*Mapping MQTT message*/
        try
        {
            map = mapper.readValue( line, Map.class );
        }
        catch( Exception e )
        {
            logger.warn( "Exception parsing mqtt message: line='" + line + "'", e );
            return ;
        }

        /*Mapping Email messaage*/
        try {
            mapBody = mapper.readValue( map.get("mqtt-body").toString().replace("\n", ""), Map.class );
        } catch (Exception e)
        {
            logger.warn( "Exception parsing email message: ", e);
            return ;
        }

        messageParameters = new EmailSender.EmailMessageParameters();
        if ( map.get( "contentType" ) != null ) {
            String contentType = (String) map.get( "contentType" );
            messageParameters.setContentType( contentType );
        }

        long groupId = 0;
        if( map.get( "groupId" ) != null ) {
            groupId = Long.parseLong( String.valueOf(map.getOrDefault( "groupId", "0") ) );
        }

        setEmailParameters(groupId);
        EmailSender emailSender = new EmailSender(mailParameters);

        // Set "to" recipients from mqtt-body
        List<String> recipients = (ArrayList<String>) mapBody.get("to");
        String [] emailsList = recipients.toArray(new String[recipients.size()]);

        messageParameters.setTo(emailsList);
        messageParameters.setSubject(mapBody.get("subject").toString()); // get the Subject
        messageParameters.setMsg(mapBody.get("email-body").toString()); // get the whole Message

        // Try to send the email message
        try {
            emailSender.send(messageParameters);
        } catch (EmailException e) {
            e.printStackTrace();
        }
    }

    private void setEmailParameters(long groupId)
    {
        mailParameters = new EmailSender.SmtpParameters();
        User rootUser = null;
        if(groupId == 0){
            rootUser = UserService.getInstance().getRootUser();
            mailParameters.setHost(ConfigurationService.getAsString(rootUser, "emailSmtpHost"));
            Long emailSmtpPort = ConfigurationService.getAsLong(rootUser, "emailSmtpPort");
            mailParameters.setPort(emailSmtpPort != null ? emailSmtpPort.intValue() : 25);
            mailParameters.setSsl(ConfigurationService.getAsBoolean(rootUser, "emailSmtpSsl"));
            mailParameters.setTls(ConfigurationService.getAsBoolean(rootUser, "emailSmtpTls"));
            mailParameters.setUserName(ConfigurationService.getAsString(rootUser, Constants.EMAIL_SMTP_USER));
            mailParameters.setPassword(ConfigurationService.getAsString(rootUser, "emailSmtpPassword"));
        } else{
            Group group = GroupService.getInstance().get(groupId);
            mailParameters.setHost(ConfigurationService.getAsString(group, "emailSmtpHost"));
            Long emailSmtpPort = ConfigurationService.getAsLong(group, "emailSmtpPort");
            mailParameters.setPort(emailSmtpPort != null ? emailSmtpPort.intValue() : 25);
            mailParameters.setSsl(ConfigurationService.getAsBoolean(group, "emailSmtpSsl"));
            mailParameters.setTls(ConfigurationService.getAsBoolean(group, "emailSmtpTls"));
            mailParameters.setUserName(ConfigurationService.getAsString(group, Constants.EMAIL_SMTP_USER));
            mailParameters.setPassword(ConfigurationService.getAsString(group, "emailSmtpPassword"));
        }

        messageParameters.setFrom(mailParameters.getUserName());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void shutdown()
    {
        logger.info("shutting down NotificationService");
        try
        {
            client.disconnect();
        }
        catch( MqttException e )
        {
            logger.warn( "e=" + e );
        }
        t.interrupt();
    }
}
