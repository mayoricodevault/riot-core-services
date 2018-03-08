package com.tierconnect.riot.iot.services;

import com.sun.jna.Platform;
import com.tierconnect.riot.appcore.entities.Version;
import com.tierconnect.riot.appcore.services.VersionService;
import com.tierconnect.riot.appcore.utils.LoggingUtils;
import com.tierconnect.riot.appcore.utils.VersionUtils;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.appcore.version.DBVersion;
import com.tierconnect.riot.commons.services.broker.BrokerConnection;
import com.tierconnect.riot.commons.services.broker.MQTTEdgeConnectionPool;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.hibernate.Session;
import org.hibernate.Transaction;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * this class is used to listen for health and status message from the mqtt bus
 * 
 * @author tcrown
 * 
 */
public class HealthAndStatusService implements MqttCallback
{
	private static final Logger logger = Logger.getLogger( HealthAndStatusService.class );
	public static final String RIOT_CORE_SERVICES = "SERVICES";

	private static List<HealthAndStatusService> services = new ArrayList<>();

	MqttClient client;

	public List<Value> values = new ArrayList<Value>();

	// KEY: bridegeCode, loggerName VALUE: the logging level (INFO,WARN,etc)
	Map<String, Map<String, String>> loggers = new TreeMap<String, Map<String, String>>();

	String fname = "healthAndStatusCache";

	long maxDataAge = 12L * 60L * 60L * 1000L;

	Thread t;

	long timelastSave;

    public String connectionCode = "";
    public int qos = 2;



	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		String host = args[0];
		int port = Integer.parseInt( args[1] );
		init( "APP-SUB-"+ UUID.randomUUID().toString());
	}

	public static void init( String clientId ) {
		Map<String, BrokerConnection> mapConnections = MQTTEdgeConnectionPool.getInstance().getAllLstConnections();
		if ( !mapConnections.isEmpty() ){
			StringBuilder sb = new StringBuilder();
			sb.append("HealthAndStatusService: Connections loaded:\n");
			for (Map.Entry<String, BrokerConnection> map : mapConnections.entrySet()){
				BrokerConnection connection = map.getValue();
				HealthAndStatusService service = new HealthAndStatusService(
						connection.getCode(), Integer.parseInt(connection.getProperties().get("qos").toString()));
				try {
					service.loadFromFile();
				} catch (IOException e) {
					logger.info("did not load file e=" + e);
				}
				service.timelastSave = System.currentTimeMillis();
				service.connect(
						connection.getProperties().get("host").toString(),
						Integer.parseInt(connection.getProperties().get("port").toString()),
						"HS-" + connection.getCode() + "-" + clientId,
						connection.getProperties().containsKey("username")?
								connection.getProperties().get("username").toString():null,
						connection.getPassword());
				service.start();
				services.add(service);
				sb.append(connection.toString()+"\n");
			}
			logger.info(sb.toString());
		} else {
			logger.error("No initialize HealthAndStatusService. Verify the following causes:\n " +
					"1. There is no a bridge configuration (type:core) registered.\n" +
					"2. Some  bridge configuration (type:core) does not have a broker connection(mqtt) assigned.");
		}
    }

	public static List<HealthAndStatusService> getInstances()
	{
		return services;
	}

	public HealthAndStatusService(String connectionCode, int qos)
	{
        this.connectionCode = connectionCode;
        this.qos = qos;
        this.qos = qos;
	}

	public void shutdown()
	{
		logger.info( "shutting down HealthAndStatusService" );
		try
		{
			client.disconnect();
		}
		catch( MqttException e )
		{
			logger.warn( "e=" + e );
		}
		t.interrupt();
		try
		{
			this.saveToFile();
		}
		catch( FileNotFoundException e )
		{

		}
	}

	public int connect( String mqtthost, int mqttport, String clientId, String mqttUsername, String mqttPassword  )
	{
		try
		{
			String url = "tcp://" + mqtthost + ":" + mqttport;
			client = new MqttClient( url, clientId, new MemoryPersistence());

			MqttConnectOptions co = new MqttConnectOptions();
			if (mqttUsername != null && !mqttUsername.isEmpty() && mqttPassword != null && !mqttPassword.isEmpty()) {
				co.setUserName(mqttUsername);
				co.setPassword(mqttPassword.toCharArray());
			}

			client.connect(co);
			logger.info( String.format( "Connected to mqtt broker: url=%s clientId=%s", url, clientId ) );

			subscribe();
		}
		catch( MqttException e )
		{
            logger.error("Error connecting to MQTT broker " , e);
			return 1;
		}
		return 0;
	}

	public void start()
	{
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
						logger.info( "client is not connected, attempting reconnection ..." );
						try
						{
							client.connect();
							logger.info( "reconnect succeded !" );
							subscribe();
							logger.info( "subscribe succeded !" );
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

					for(StatusServiceCallback statusServiceCB : StatusService.getInstance().getStatusServiceCallbacks()){
						for(ConcurrentHashMap.Entry<String, com.tierconnect.riot.commons.utils.Timer> timer : statusServiceCB.getTimers().entrySet()){
							timer.getValue().stop(timer.getKey()+ "-status");
							final long totalInactivity = timer.getValue().getTotal(timer.getKey()+"-status");
							logger.debug("totalInactivity >>> " + totalInactivity);
							logger.debug("BridgeErrorStatusTimeout >>> " + StatusService.getInstance().getBridgeErrorStatusTimeout());
							if (totalInactivity >= StatusService.getInstance().getBridgeErrorStatusTimeout()) {
								Transaction transaction = null;
								Session session = HibernateSessionFactory.getInstance().getCurrentSession();
								transaction = session.getTransaction();
								transaction.begin();
								Edgebox edgebox = EdgeboxService.getInstance().selectByCode(timer.getKey());
								edgebox.setStatus("ERROR");
								EdgeboxService.getInstance().update(edgebox);
								transaction.commit();
								logger.debug(String.format("(%s)  INACTIVITY DETECTED (%d min)", timer.getKey(), (totalInactivity/1000)/60));
							}

						}
					}
				}
				logger.info( "health and status thread ended" );
			};
		};
		t.start();
		logger.info( "health and status thread started" );
	}

	private void subscribe() throws MqttException
	{
		// String topicSubscription = "/v1/edge/up/#";
		String topicSubscription = "/v1/status/#";

		client.subscribe( topicSubscription, qos );
		logger.info( "subscribed to topic=" + topicSubscription + " with qos=" + qos );

		client.setCallback( this );
		logger.info( "callback set" );
	}

	public boolean isConnected()
	{
		return client.isConnected();
	}

	public void disconnect( int i ) throws MqttException
	{
		client.disconnect( i );
	}

	@Override
	public void connectionLost( Throwable e )
	{
		logger.error( "connection lost: ", e );
	}

	@Override
	public void deliveryComplete( IMqttDeliveryToken arg0 )
	{

	}

	// sn=45
	// ALEB,1433733040002,uptime,438168
	// ALEB,1433733040002,heartbeatThreadCount,1
	// ALEB,1433733040002,mem.used,10
	// ALEB,1433733040002,mem.free,51
	// ALEB,1433733040002,mem.total,61
	// ALEB,1433733040002,mem.max,455
	// ALEB,1433733040002,sn[ALEB],0
	// ALEB,1433733040002,lpt,0.0
	// ALEB,1433733040002,lpt.avg1,0.0
	// ALEB,1433733040002,lpt.avg5,0.0
	// ALEB,1433733040002,lpt.avg15,0.0
	// ALEB,1433733040002,age,0.0
	// ALEB,1433733040002,age.avg1,0.0
	// ALEB,1433733040002,age.avg5,0.0
	// ALEB,1433733040002,age.avg15,0.0
	// ALEB,1433733040002,thing_count.avg1,0.0
	// ALEB,1433733040002,thing_count.avg5,0.0
	// ALEB,1433733040002,thing_count.avg15,0.0
	// ALEB,1433733040002,sent.avg1,0.0
	// ALEB,1433733040003,period.avg1,0.0
	// ALEB,1433733040003,thing_count_total,0

	@Override
	public void messageArrived( String topic, MqttMessage mqttm ) throws Exception
	{
		try
		{
			logger.debug( "topic='" + topic + "'" );

			// String[] t = topic.split( "/" );

			// String bridgeCode = t[3];

			String body = new String( mqttm.getPayload(), "UTF-8" );
			// System.out.println( "body='" + body + "'" );

			String[] lines = body.split( "\n" );

			// String line = lines[0]; // sn=12345

			synchronized( values )
			{
				for( int i = 1; i < lines.length; i++ )
				{
					parse( lines[i] );
				}
			}
		}
		catch( Exception e )
		{
			logger.warn( "Exception parsing message body: ", e );
		}

		expire();

		logger.debug( "values=" + values.size() );

		long now = System.currentTimeMillis();
		if( now - timelastSave > 5L * 60L * 1000L )
		{
			this.saveToFile();
			timelastSave = now;
		}
	}

	/**
	 * 
	 * @return max data age in milliseconds
	 */
	public long getMaxDataAge()
	{
		return maxDataAge;
	}

	// max data age in milliseconds
	public void setMaxDataAge( long maxDataAge )
	{
		this.maxDataAge = maxDataAge;
	}

	private void expire()
	{
		long time = System.currentTimeMillis() - maxDataAge;
		synchronized( values )
		{
			for( Iterator<Value> i = values.iterator(); i.hasNext(); )
			{
				Value v = i.next();
				if( v.time < time )
				{
					i.remove();
				}
			}
		}
	}

	private void parse( String line )
	{
		try
		{
			String[] tokens = line.split( "\\," );

			Value v = new Value();
			v.bridgeCode = tokens[0];
			v.time = Long.parseLong( tokens[1] );
			v.propertyName = tokens[2];
			v.value = tokens[3];
            v.connectionCode = connectionCode;

            if( v.propertyName.startsWith( "log4j-" ) )
			{
				String propertyName = v.propertyName.substring( 6 );
				addLogger( v.bridgeCode, propertyName, v.value );
			}
			else
			{
				values.add( v );
			}
		}
		catch( Exception e )
		{
			logger.warn( "Exception parsing message body: line='" + line + "'", e );
		}

	}

	private void addLogger( String bridgeCode, String propertyName, String value )
	{
		Map<String, String> m1 = loggers.get( bridgeCode );

		if( m1 == null )
		{
			m1 = new TreeMap<String, String>();
			loggers.put( bridgeCode, m1 );
		}

		m1.put(propertyName, value);
	}

	public void saveToFile() throws FileNotFoundException
	{
		saveToFile(new File(fname + "_" + connectionCode + ".txt"));
	}

	private void saveToFile( File file ) throws FileNotFoundException
	{
		TimerUtil tu = new TimerUtil();
		tu.mark();
		logger.info("file :" + file.getAbsolutePath());
		FileOutputStream fis = new FileOutputStream( file );
		PrintWriter pw = new PrintWriter(new OutputStreamWriter( fis,Charsets.UTF_8));

		synchronized( values )
		{
			for( Value v : values )
			{
				pw.write( v.bridgeCode + "," + v.time + "," + v.propertyName + "," + v.value + "\n" );
			}
		}

		pw.close();

		tu.mark();
		logger.info("wrote " + values.size() + " records in " + tu.getLastDelt() + " ms to file='" + file.getAbsolutePath() + "'");
	}

	public void loadFromFile() throws IOException
	{
		loadFromFile( new File( fname + "_" + connectionCode + ".txt" ) );

	}

	private void loadFromFile( File file ) throws IOException
	{
		if( file.exists() )
		{
			TimerUtil tu = new TimerUtil();
			tu.mark();
			FileInputStream fis = new FileInputStream( file );
			InputStreamReader isr = new InputStreamReader( fis, Charsets.UTF_8);
			BufferedReader br = new BufferedReader( isr );
			synchronized( values )
			{
				values.clear();
				String line;
				while( (line = br.readLine()) != null )
				{
					parse( line );
				}
			}
			fis.close();
			expire();
			tu.mark();
			logger.info( "loaded " + values.size() + " records in " + tu.getLastDelt() + " ms from file='" + file.getAbsolutePath() + "'" );
		}
	}

	public class Value
	{
		public String bridgeCode;
		public String connectionCode;
		public String propertyName;
		public long time;
		public String value;
	}

	public String getLoggers()
	{
		StringBuffer sb = new StringBuffer();

		for( String bridgeCode : loggers.keySet() )
		{
			Map<String, String> m = loggers.get( bridgeCode );
			for( String loggerName : m.keySet() )
			{
				if( loggerName.startsWith( "com.tierconnect" ) )
				{
					sb.append( bridgeCode + "," + loggerName + "," + m.get( loggerName ) + "\n" );
				}
			}
		}

		Map<String, Logger> map = LoggingUtils.getLoggers();
		for( Logger l : map.values() )
		{
			if( l.getName().startsWith( "com.tierconnect" ) ) {
				sb.append(RIOT_CORE_SERVICES + "," + l.getName() + "," + l.getEffectiveLevel() + "\n");
			}
		}

		return sb.toString();
	}

	// This method sets the log4j logging levels on the bridges using mqtt messages.
	// It handles multiple bridgeCodes.
	// TODO: handle riot-core-services loggers too !
	public void setLoggers( String body )
	{
		String bridgeCode = null;
		Map<String, StringBuffer> map = new HashMap<String, StringBuffer>();

		String[] lines = body.split( "\n" );
		for( String line : lines )
		{
			logger.info( "line='" + line + "'" );
			String[] tokens = line.split( "," );
			bridgeCode = tokens[0];
			if (RIOT_CORE_SERVICES.equals(bridgeCode)) {
				LoggingUtils.setLoggingLevel(tokens[1] + "=" + tokens[2] + "\n");
				continue;
			}

			StringBuffer sb = map.get( bridgeCode );

			if( sb == null )
			{
				sb = new StringBuffer();
				map.put( bridgeCode, sb );
			}

			// Updating the loggers Map
			Map<String, String> m = loggers.get( bridgeCode );
			for( Entry<String, String> loggerEntry : m.entrySet() )
			{
				if( loggerEntry.getKey().startsWith("com.tierconnect") && loggerEntry.getKey().equals(tokens[1]) )
				{
					String keyToSet = loggerEntry.getKey();
					m.put( keyToSet, tokens[2] );
				}
			}
			loggers.put( bridgeCode, m );

			sb.append( tokens[1] + "=" + tokens[2] + "\n" );
		}

		for( Entry<String, StringBuffer> e : map.entrySet() )
		{
			BrokerClientHelper.sendLoggers( e.getKey(), e.getValue().toString(), true, null );
		}
	}

	// private String format( long seconds )
	// {
	// int day = (int) TimeUnit.SECONDS.toDays( seconds );
	// long hours = TimeUnit.SECONDS.toHours( seconds ) - (day * 24);
	// long minute = TimeUnit.SECONDS.toMinutes( seconds ) -
	// (TimeUnit.SECONDS.toHours( seconds ) * 60);
	// long second = TimeUnit.SECONDS.toSeconds( seconds ) -
	// (TimeUnit.SECONDS.toMinutes( seconds ) * 60);
	// return String.format( "%dd %02dh %02dm %02ds", day, hours, minute, second
	// );
	// }

	/**
	 * Method to get some application information: migrations, core-services dbversion, timezone
	 * @return
     */
	public static Map<String,Object> getApplicationInfo(){
		Map<String,Object> result = new HashMap<>();

		// migrations
		List<Version> versions = VersionService.getVersionDAO().selectAll();
		if (versions != null && !versions.isEmpty()){
			List<Map> migrations = new ArrayList<>();
			for (Version version : versions){
				String dbVersion = version.getDbVersion();
				Long dbVersionNumber = Long.parseLong(dbVersion);
				StringBuilder dbVersionSB = new StringBuilder(dbVersion);

				Map<String,Object> migration = new HashMap<>();
				migration.put("dbVersionNumber",dbVersionNumber);
				dbVersionSB.setCharAt(1, '.');
				dbVersionSB.setCharAt(3, '.');
				migration.put("dbVersion",dbVersionSB.toString());
				migration.put("installTime",version.getInstallTime().toString());
				migrations.add(migration);
			}
			result.put("migrations",migrations);
		}

		// version
		int currentDBVersion = DBVersion.getInstance().getDbVersion();
		int targetVersion = CodeVersion.getInstance().getCodeVersion();
		Map<String,Object> version = new HashMap<>();
		version.put("databaseVersion", VersionUtils.getAppVersionString(currentDBVersion));
		version.put("coreServicesVersion", VersionUtils.getAppVersionString(targetVersion));
		version.put("databaseVersionNumber",currentDBVersion);
		version.put("coreServicesVersionNumber",targetVersion);
		result.put("version",version);

		// server
		Map<String,Object> server = new HashMap<>();

		// timezone
		Map<String,Object> timezone = new HashMap<>();
		Calendar calendar = new GregorianCalendar();
		TimeZone timeZone = calendar.getTimeZone();
		timezone.put("timezone",timeZone);
		Long timeZoneOffset = (new Date()).getTimezoneOffset()*60000L;
		timezone.put("timeZoneOffset",timeZoneOffset);
		server.put("timezone",timezone);

		// CPU
		Map<String,Object> cpu = new HashMap<>();
		SystemInfo si = new SystemInfo();
		OperatingSystem os = si.getOperatingSystem();
		server.put("OS",os);
		HardwareAbstractionLayer hal = si.getHardware();
        if (Platform.isLinux()) {
            cpu.put("cpuLoadAverage",hal.getProcessor().getSystemLoadAverage());
        } else if (Platform.isWindows()) {
            cpu.put("cpuLoad",hal.getProcessor().getSystemCpuLoad());
        }
		server.put("cpu",cpu);

		// Memory
		Map<String,Object> ram = new HashMap<>();
		ram.put("total",FormatUtil.formatBytes(hal.getMemory().getTotal()));
		ram.put("free",FormatUtil.formatBytes(hal.getMemory().getAvailable()));
		server.put("ram",ram);

		result.put("server",server);
		return result;
	}

}
