package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.commons.dtos.ConnectionDto;
import com.tierconnect.riot.commons.dtos.EdgeboxConfigurationDto;
import com.tierconnect.riot.commons.dtos.EdgeboxDto;
import com.tierconnect.riot.commons.dtos.EdgeboxRuleDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ShiftZoneDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.dtos.ZoneTypeDto;
import com.tierconnect.riot.commons.serdes.JsonSerde;
import com.tierconnect.riot.commons.serdes.JsonSerializer;
import com.tierconnect.riot.commons.utils.Topics;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.KeyGenEntities;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.tierconnect.riot.iot.entities.Shift;
import com.tierconnect.riot.iot.entities.ShiftZone;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.iot.utils.Translator;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

/**
 * CacheLoaderTool class.
 *
 * @author tcrown
 * @author jantezana
 * @author vramos
 * 
 * @version 2017/01/11
 * 
 *          New version first reads from Mysql and Mongo, and writes to files on disk.
 * 
 *          Then, it reads from those files on disk, and writes them to Kafka.
 * 
 *          The files on disk can be used as the basis for loaders to other pub-sub servcies, e.g. Azure, Amazon, etc.
 * 
 */
public class CacheLoaderToolNew
{
	private static Logger logger = Logger.getLogger( CacheLoaderToolNew.class );

	private static final String DIR_CONNECTION = "connection";
	private static final String DIR_EDGEBOX = "edgebox";
	private static final String DIR_EDGEBOX_CONFIG = "edgeboxconfiguration";
	private static final String DIR_GROUP = "group";
	private static final String DIR_GROUP_TYPE = "groupType";
	private static final String DIR_EDGEBOX_RULE = "edgeboxrule";
	private static final String DIR_LOGICAL_READER = "logicalreader";
	private static final String DIR_SHIFT = "shift";
	private static final String DIR_SHIFT_ZONE = "shiftzone";
	private static final String DIR_THINGS = "things";
	private static final String DIR_THING_TYPE = "thingtype";
	private static final String DIR_ZONE = "zone";
	private static final String DIR_ZONE_TYPE = "zonetype";

	ClassLoaderToolArgsNew args;

	private MysqlDataProvider mysqlProvider;

	private MongoDataProvider mongoProvider;

	private KafkaProducer<String, Object> producer;

	File outdir;

	List<Group> groups;
	List<GroupDto> groupDtos;
	List<GroupTypeDto> groupTypeDtos;
	List<LogicalReaderDto> logicalReaderDtos;
	List<ShiftDto> shiftDtos;
	List<ThingTypeDto> thingTypeDtos;
	List<ZoneDto> zoneDtos;

	public static void main( String args[] ) throws Exception
	{
		CacheLoaderToolNew clt = new CacheLoaderToolNew( args );
		clt.run();
	}

	public CacheLoaderToolNew( String[] sargs )
	{
		args = new ClassLoaderToolArgsNew();
		args.init( sargs );
		outdir = new File( args.outdir );
	}

	public void run() throws Exception
	{
		logger.info( "*******************************************" );
		logger.info( "*****        CacheLoaderTool           ****" );
		logger.info( "*******************************************" );

		if( args.dl )
		{
			initMysqlProvider();
			if( args.contains( DIR_THINGS ) )
				initMongoProvider();
			loadEntities();
		}

		if( args.pk )
		{
			initProducer();
			publishKafka();
			producer.close();
		}
	}

	/**
	 * Initializes data provider objects for mysql and mongo.
	 */
	private void initMysqlProvider() throws Exception
	{
		System.getProperties().put( "hibernate.hbm2ddl.auto", "update" );
		System.getProperties().put( "hibernate.cache.use_second_level_cache", "false" );
		System.getProperties().put( "hibernate.cache.use_query_cache", "false" );
		Class.forName( "org.gjt.mm.mysql.Driver" );
		mysqlProvider = new MysqlDataProvider();
	}

	private void initMongoProvider() throws Exception
	{
		MongoConfig mc = new MongoConfig();
		mc.mongoPrimary = System.getProperty( "mongo.primary" );
		mc.mongoSecondary = System.getProperty( "mongo.secondary" );
		mc.mongoReplicaSet = System.getProperty( "mongo.replicaset" );
		mc.mongoSSL = Boolean.valueOf( System.getProperty( "mongo.ssl" ) );
		mc.username = System.getProperty( "mongo.username" );
		mc.password = System.getProperty( "mongo.password" );
		mc.mongoAuthDB = System.getProperty( "mongo.authdb" );
		mc.mongoDB = System.getProperty( "mongo.db" );
		mc.mongoSharding = Boolean.valueOf( System.getProperty( "mongo.sharding" ) );
		mc.mongoConnectTimeout = Integer.parseInt( System.getProperty( "mongo.connectiontimeout" ) );
		mc.mongoMaxPoolSize = Integer.parseInt( System.getProperty( "mongo.maxpoolsize" ) );
		mongoProvider = new MongoDataProvider( mc );
	}

	/**
	 * Initializes kafka producer
	 *
	 * @throws CacheLoaderException
	 */
	private void initProducer() throws Exception
	{
		Properties p = new Properties();
		p.put( "bootstrap.servers", System.getProperty( "kafka.servers" ) );
		p.put( "batch.size", 131072 );
		p.put( "linger.ms", 20 );
		p.put( "key.serializer", "org.apache.kafka.common.serialization.StringSerializer" );
		p.put( "value.serializer", JsonSerializer.class );
		producer = new KafkaProducer<>( p );
	}

	/**
	 * Loads cache entities from mysql and publish them into kafka ___v1___cache___*
	 */
	private void loadEntities() throws Exception
	{
		FileUtils.deleteDirectory( outdir );

		Transaction transaction = HibernateSessionFactory.getInstance().getCurrentSession().getTransaction();
		transaction.begin();

		// Connection
		if( args.contains( DIR_CONNECTION ) )
			loadConnections();

		// Group
		if( args.contains( DIR_GROUP ) )
			loadGroups(); // must do groups before edgeboxes

		// EdgeBox
		if( args.contains( DIR_EDGEBOX ) )
			loadEdgeboxes( null );

		// EdgeBoxRule
		if( args.contains( DIR_EDGEBOX_RULE ) )
			loadEdgeboxRules();

		// GroupType
		if( args.contains( DIR_GROUP_TYPE ) )
			loadGroupTypes();

		// ThingTypes
		if( args.contains( DIR_THING_TYPE ) )
			loadThingTypes( null );

		// ZoneTypes
		if( args.contains( DIR_ZONE_TYPE ) )
			loadZoneTypes();

		logger.info( "Loading things..." );
		int batchSize = 128;
		Map<Long, Long> dataTypesByThingField = buildDataTypesByThingField();
		List<Group> tenants = mysqlProvider.getTenants();
		for( Group tenant : tenants )
		{
			logger.info( "### Loadding things for tenant: " + tenant.getCode() );

			// Logical Reader
			if( args.contains( DIR_LOGICAL_READER ) )
				loadLogicalReaders( tenant );

			// Shift
			if( args.contains( DIR_SHIFT ) )
				loadShifts( tenant );

			// Shift Zones
			if( args.contains( DIR_SHIFT_ZONE ) )
				loadShiftZones( tenant );

			// Zones
			if( args.contains( DIR_ZONE ) )
				loadZones( tenant );

			if( args.contains( DIR_THINGS ) )
			{
				List<Long> groupIds = mysqlProvider.getTenantChildrenIds( tenant );
				DBCursor thingsCursor = mongoProvider.getThings( groupIds.toArray( new Long[groupIds.size()] ) );

				int total = 0;
				while( thingsCursor.hasNext() )
				{
					int count = 0;
					List<ThingDto> list = new ArrayList<>();
					while( count < batchSize && thingsCursor.hasNext() )
					{
						BasicDBObject thingMongo = (BasicDBObject) thingsCursor.next();
						try
						{
							ThingDto thingDto = MongoDataProvider.buildThingDto( thingMongo, dataTypesByThingField, zoneDtos, logicalReaderDtos, shiftDtos, thingTypeDtos, groupDtos, groupTypeDtos );
							list.add( thingDto );
							count++;
						}
						catch( Exception e )
						{
							logger.error( "Unable to load thing: " + thingMongo, e );
						}
					}
					total += list.size();
					logger.info( String.format( "Loading %d things. %d loaded things until now. ", list.size(), total ) );
					List<ThingDto> thingPairs = Translator.convertToThingPairs( list, groups );
					loadThings( thingPairs, tenant );
				}
				logger.info( String.format( "Total things loaded: %d ", total ) );
			}
		}

		transaction.commit();
	}

	/**
	 * load connections
	 */
	private void loadConnections() throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading connections..." );
		List<Connection> connections = mysqlProvider.getConnections();
		List<ConnectionDto> connectionDtos = Translator.convertToConnectionDTOs( connections );
		Serde<ConnectionDto> serdes = new JsonSerde<>( ConnectionDto.class );
		for( ConnectionDto dto : connectionDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_CONNECTION, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_CONNECTION, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d connections in %d milliseconds", connectionDtos.size(), end - start ) );
	}

	/**
	 * load edgeBoxes
	 */
	private void loadEdgeboxes( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();

		logger.info( "Loading Edgeboxes..." );
		List<Edgebox> edgeboxes = mysqlProvider.getEdgeBoxes( tenant );
		List<EdgeboxDto> edgeboxDtos = Translator.convertToEdgeboxDTOs( edgeboxes );
		List<EdgeboxConfigurationDto> edgeBoxConfigurations = new ArrayList<>();
		Serde<EdgeboxDto> serdes = new JsonSerde<>( EdgeboxDto.class );

		for( EdgeboxDto dto : edgeboxDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_EDGEBOX, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_EDGEBOX, key, value );
		}
		serdes.close();

		for( EdgeboxDto edgeboxDto : edgeboxDtos )
		{
			String edgeboxConfiguration = mysqlProvider.getConfiguration( edgeboxDto.code );
			EdgeboxConfigurationDto edgeboxConfigurationDto = new EdgeboxConfigurationDto();
			edgeboxConfigurationDto.id = edgeboxDto.id;
			edgeboxConfigurationDto.code = edgeboxDto.code;
			edgeboxConfigurationDto.type = edgeboxDto.type;
			edgeboxConfigurationDto.configuration = edgeboxConfiguration;
			GroupDto groupDto = new GroupDto();
			groupDto.id = edgeboxDto.group.id;
			edgeboxConfigurationDto.group = groupDto;
			edgeBoxConfigurations.add( edgeboxConfigurationDto );
		}

		Serde<EdgeboxConfigurationDto> serdes2 = new JsonSerde<>( EdgeboxConfigurationDto.class );
		for( EdgeboxConfigurationDto dto : edgeBoxConfigurations )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes2.serializer().serialize( null, dto );
			writeToFile( DIR_EDGEBOX_CONFIG, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_EDGEBOX_CONFIG, key, value );
		}
		serdes2.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d edgeboxes in %d milliseconds", edgeboxDtos.size(), end - start ) );
	}

	/**
	 * load edge box rules
	 */
	private void loadEdgeboxRules() throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading edgeboxRules..." );
		List<EdgeboxRule> edgeboxRules = mysqlProvider.getEdgeBoxRules();
		List<EdgeboxRuleDto> edgeboxRuleDtos = Translator.convertToEdgeboxRuleDTOs( edgeboxRules );
		Serde<EdgeboxRuleDto> serdes = new JsonSerde<>( EdgeboxRuleDto.class );
		for( EdgeboxRuleDto dto : edgeboxRuleDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_EDGEBOX_RULE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d edge box rules in %d milliseconds", edgeboxRuleDtos.size(), end - start ) );
	}

	/**
	 * load groups
	 */
	private void loadGroups() throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading groups..." );
		groups = mysqlProvider.getGroups();
		groupDtos = Translator.convertToGroupDTOs( groups );
		Serde<GroupDto> serdes = new JsonSerde<>( GroupDto.class );
		for( GroupDto dto : groupDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_GROUP, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_GROUP, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d groups in %d milliseconds", groupDtos.size(), end - start ) );
	}

	/**
	 * load groupTypes
	 */
	private void loadGroupTypes() throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading groupTypes..." );
		List<GroupType> groupTypes = mysqlProvider.getGroupTypes();
		groupTypeDtos = Translator.convertToGroupTypeDTOs( groupTypes );
		Serde<GroupTypeDto> serdes = new JsonSerde<>( GroupTypeDto.class );
		for( GroupTypeDto dto : groupTypeDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_GROUP_TYPE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d groupTypes in %d milliseconds", groupTypeDtos.size(), end - start ) );
	}

	/**
	 * load logicalReaders
	 */
	private void loadLogicalReaders( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading LogicalReaders..." );
		List<LogicalReader> logicalReaders = mysqlProvider.getLogicalReaders( tenant );
		logicalReaderDtos = Translator.convertToLogicalReaderDTOs( logicalReaders );
		Serde<LogicalReaderDto> serdes = new JsonSerde<>( LogicalReaderDto.class );
		for( LogicalReaderDto dto : logicalReaderDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_LOGICAL_READER, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_LOGICAL_READER, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d logical readers in %d milliseconds", logicalReaderDtos.size(), end - start ) );
	}

	/**
	 * load shifts
	 */
	private void loadShifts( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading Shifts..." );
		List<Shift> shifts = mysqlProvider.getShifts( tenant );
		shiftDtos = Translator.convertToShiftDTOs( shifts );
		Serde<ShiftDto> serdes = new JsonSerde<>( ShiftDto.class );
		for( ShiftDto dto : shiftDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_SHIFT, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_SHIFT, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d shifts in %d milliseconds", shiftDtos.size(), end - start ) );
	}

	/**
	 * load shift zones
	 */
	private void loadShiftZones( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading ShiftZones..." );
		List<ShiftZone> shiftZones = mysqlProvider.getShiftZones( tenant );
		List<ShiftZoneDto> shiftZoneDtos = Translator.convertToShiftZoneDTOs( shiftZones, new HashMap<>() );
		Serde<ShiftZoneDto> serdes = new JsonSerde<>( ShiftZoneDto.class );
		for( ShiftZoneDto dto : shiftZoneDtos )
		{
			// String idKey = "ID-" + String.valueOf(shiftZoneDto.shift.id);
			// String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + String.valueOf(shiftZoneDto.shift.id);
			//TODO: is this correct !?!
			String key = KeyGenEntities.getId( dto.shift );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_SHIFT_ZONE, key, value );
			//key = "CODE-" + getTenantCode( tenant ) + dto.shift.id;
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_SHIFT_ZONE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d shift zones in %d milliseconds", shiftZoneDtos.size(), end - start ) );
	}

	/**
	 * load things
	 */
	public void loadThings( List<ThingDto> thingDtos, Group tenant )
	{
		long start = System.currentTimeMillis();
		Serde<ThingDto> serdes = new JsonSerde<>( ThingDto.class );
		for( ThingDto dto : thingDtos )
		{
			// String key = (tenant != null ? (tenant.getCode() + "-") : "") + thingDto.thingType.code + "-" + thingDto.serialNumber;
			String key = getTenantCode( tenant ) + dto.thingType.code + "-" + dto.serialNumber;
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_THINGS, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d things in %d milliseconds", thingDtos.size(), end - start ) );
	}

	/**
	 * load thingTypes
	 */
	private void loadThingTypes( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "loading thing types..." );
		List<ThingType> thingTypes = mysqlProvider.getThingTypes( tenant );
		thingTypeDtos = Translator.convertToThingTypeDTOs( thingTypes );
		Serde<ThingTypeDto> serdes = new JsonSerde<>( ThingTypeDto.class );
		for( ThingTypeDto dto : thingTypeDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_THING_TYPE, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_THING_TYPE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d thing types in %d milliseconds", thingTypeDtos.size(), end - start ) );
	}

	/**
	 * load zones
	 */
	private void loadZones( Group tenant ) throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading zones..." );
		Map<Long, Map<String, Object>> properties = mysqlProvider.getZoneProperties();
		List<Zone> zones = mysqlProvider.getZones( tenant );
		zoneDtos = Translator.convertToZoneDTOs( zones, properties );
		Serde<ZoneDto> serdes = new JsonSerde<>( ZoneDto.class );
		for( ZoneDto dto : zoneDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_ZONE, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_ZONE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d zones in %d milliseconds", zoneDtos.size(), end - start ) );
	}

	/**
	 * load zone types
	 */
	private void loadZoneTypes() throws Exception
	{
		long start = System.currentTimeMillis();
		logger.info( "Loading zone types..." );
		List<ZoneType> zoneTypes = mysqlProvider.getZoneTypes();
		List<ZoneTypeDto> zoneTypeDtos = Translator.convertToZoneTypeDTOs( zoneTypes );
		Serde<ZoneTypeDto> serdes = new JsonSerde<>( ZoneTypeDto.class );
		for( ZoneTypeDto dto : zoneTypeDtos )
		{
			String key = KeyGenEntities.getId( dto );
			byte[] value = serdes.serializer().serialize( null, dto );
			writeToFile( DIR_ZONE_TYPE, key, value );
			key = KeyGenEntities.getCode( dto );
			writeToFile( DIR_ZONE_TYPE, key, value );
		}
		serdes.close();
		long end = System.currentTimeMillis();
		logger.info( String.format( "Loaded %d zone types in %d milliseconds", zoneTypeDtos.size(), end - start ) );
	}

	private String getTenantCode( Group tenant )
	{
		return(tenant != null ? (tenant.getCode() + "-") : "");
	}

	private void writeToFile( String dirname, String key, byte[] value )
	{
		try
		{
			File dir = new File( outdir, dirname );
			if( !dir.exists() )
			{
				dir.mkdirs();
			}
			File f = new File( dir, key );
			FileOutputStream fos = new FileOutputStream( f );
			fos.write( value );
			fos.close();
		}
		catch( Exception e )
		{
			logger.error( "error: ", e );
		}
	}

	private void publishKafka()
	{
		File[] dirs = outdir.listFiles();
		for( File dir : dirs )
		{
			if( dir.isDirectory() )
			{
				long start = System.currentTimeMillis();
				File[] files = dir.listFiles();
				for( File file : files )
				{
					String key = file.getName();
					logger.info( "publishing " + file.getAbsolutePath() );
					try
					{
						byte[] bytes = Files.readAllBytes( Paths.get( file.getAbsolutePath() ) );

						switch( dir.getName() )
						{
							case DIR_CONNECTION:
								publishKafka( Topics.CACHE_CONNECTION, key, bytes, new JsonSerde<>( ConnectionDto.class ) );
								break;

							case DIR_EDGEBOX:
								publishKafka( Topics.CACHE_EDGEBOX, key, bytes, new JsonSerde<>( EdgeboxDto.class ) );
								break;

							case DIR_EDGEBOX_CONFIG:
								publishKafka( Topics.CACHE_EDGEBOXES_CONFIGURATION, key, bytes, new JsonSerde<>( EdgeboxConfigurationDto.class ) );
								break;

							case DIR_EDGEBOX_RULE:
								publishKafka( Topics.CACHE_EDGEBOX_RULE, key, bytes, new JsonSerde<>( EdgeboxRuleDto.class ) );
								break;

							case DIR_GROUP:
								publishKafka( Topics.CACHE_GROUP, key, bytes, new JsonSerde<>( GroupDto.class ) );
								break;

							case DIR_GROUP_TYPE:
								publishKafka( Topics.CACHE_GROUP_TYPE, key, bytes, new JsonSerde<>( GroupTypeDto.class ) );
								break;

							case DIR_LOGICAL_READER:
								publishKafka( Topics.CACHE_LOGICAL_READER, key, bytes, new JsonSerde<>( LogicalReaderDto.class ) );
								break;

							case DIR_SHIFT:
								publishKafka( Topics.CACHE_SHIFT, key, bytes, new JsonSerde<>( ShiftDto.class ) );
								break;

							case DIR_SHIFT_ZONE:
								publishKafka( Topics.CACHE_SHIFT_ZONE, key, bytes, new JsonSerde<>( ShiftZoneDto.class ) );
								break;

							case DIR_THINGS:
								publishKafka( Topics.CACHE_THING, key, bytes, new JsonSerde<>( ThingDto.class ) );
								break;

							case DIR_THING_TYPE:
								publishKafka( Topics.CACHE_THING_TYPE, key, bytes, new JsonSerde<>( ThingTypeDto.class ) );
								break;

							case DIR_ZONE:
								publishKafka( Topics.CACHE_ZONE, key, bytes, new JsonSerde<>( ZoneDto.class ) );
								break;

							case DIR_ZONE_TYPE:
								publishKafka( Topics.CACHE_ZONE_TYPE, key, bytes, new JsonSerde<>( ZoneTypeDto.class ) );
								break;

							default:
								throw new Error( "unhandled case, directory=" + dir.getName() );
						}
					}
					catch( Exception e )
					{
						logger.info( "error: ", e );
					}
				}
				long end = System.currentTimeMillis();
				logger.info( String.format( "Wrote %d records in %d milliseconds", files.length, end - start ) );
			}
		}
	}

	private void publishKafka( Topics topic, String key, byte[] bytes, Serde<?> serdes ) throws Exception
	{
		Object value = serdes.deserializer().deserialize( "", bytes );
		ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>( topic.getKafkaName(), key, value );
		this.producer.send( producerRecord );
	}

	/**
	 * build data types by thing field.
	 */
	// TODO: move this out of here !!!
	private Map<Long, Long> buildDataTypesByThingField() throws Exception
	{
		Map<Long, Long> dataTypesByThingField = new HashMap<>();
		List<ThingType> thingTypes = mysqlProvider.getThingTypes();
		thingTypes.forEach( tt ->
		{
			tt.getThingTypeFields().forEach( ttf ->
			{
				dataTypesByThingField.put( ttf.getId(), ttf.getDataType().getId() );
			} );
		} );
		return dataTypesByThingField;
	}
}
