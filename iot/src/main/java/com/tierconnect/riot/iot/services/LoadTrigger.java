package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.RiotMessageBuilder;
import com.tierconnect.riot.commons.utils.Topics;
import org.apache.log4j.Logger;

import java.util.List;

public class LoadTrigger
{
	private static Logger logger = Logger.getLogger( LoadTrigger.class );

	boolean started = false;

	long startTime;

	long currentTime;

	long delt = 5000;

	String topic;

	StringBuffer body;
	List<Long> groupMqtt;

	long sequenceNumber = 1;

	public RiotMessageBuilder riotMessageBuilder;
	public String outputFormat;

    @Deprecated
	public void tickle( String bridgeCode, String thingTypeCode, String serial, List<Long> groupMqtt,
						long time, String name, String value )
	{
		synchronized( this )
		{
			logger.debug( "tickle" );

			if( this.started )
			{
				this.startTime = System.currentTimeMillis();
			}
			else
			{
				this.topic = "/v1/data/" + bridgeCode + "/" + thingTypeCode;
				this.groupMqtt = groupMqtt;
				this.body = new StringBuffer();
				this.body.append( "sn," + this.sequenceNumber + "\n" );
				this.sequenceNumber++;
				start();
			}

			this.body.append( serial + "," + time + "," + name + "," + value + "\n" );
		}
	}

    @Deprecated
	private void start()
	{
		started = true;

		startTime = System.currentTimeMillis();

		Thread t = new Thread()
		{
			public void run()
			{
				while( started )
				{
					try
					{
						Thread.sleep( 100 );
					}
					catch( InterruptedException e )
					{
						e.printStackTrace();
					}

					synchronized( this )
					{
						long now = System.currentTimeMillis();
						if( now - startTime > delt )
						{
							callback(true, null);
							started = false;
						}
					}
				}
			}
		};

		t.start();
	}

	// temporary hack
    @Deprecated
	public void goNow()
	{
		startTime = 0;
	}
	
	public void callback(boolean publishMessage, String threadName)
	{

	}

    /**
     * Start the message that will be sent to coreBridge
     * @param bridgeCode String
     * @param thingTypeCode String
     */
    public void initThingFieldTickle(String bridgeCode, String thingTypeCode, List<Long> groupMqtt)
    {
		this.riotMessageBuilder = new RiotMessageBuilder();
		String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
		boolean kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;
    	logger.info(String.format("kafkaEnabled : %s", kafkaEnabled));


		if(kafkaEnabled) {
			topic = Topics.DATA_1.getKafkaName();
		}else {
			topic = "/v1/data/" + bridgeCode + "/" + thingTypeCode;
		}


	  // TODO: Old Implementation.
		this.groupMqtt = groupMqtt;
		body = new StringBuffer();
		body.append( "sn,").append(sequenceNumber).append("\n");

		this.riotMessageBuilder.setBridgeCode(bridgeCode);
		this.riotMessageBuilder.setThingTypeCode(thingTypeCode);
		this.riotMessageBuilder.setSqn(sequenceNumber);
		this.riotMessageBuilder.setSpecName(Constants.SOURCE_SERVICE);

        sequenceNumber++;
    }

	public void initThingFieldTickleKafka(String bridgeCode, String thingTypeCode, Boolean runRules)
	{
		initThingFieldTickle(bridgeCode, thingTypeCode, null);
		this.riotMessageBuilder.setRunRules(runRules);
	}

    /**
     * Add fields to the message that will be sent to the coreBridge
     * @param serial String
     * @param time long
     * @param name String
     * @param value String
     */
    public void setThingField(String serial, long time, String name, String value)
    {
    	// TODO: Old Implementation.
			body.append(serial).append(",")
					.append(time).append( ",")
					.append(name).append(",")
					.append(value).append("\n");

			this.riotMessageBuilder.setSerialNumber(serial);
			this.riotMessageBuilder.addProperty(time, name, value);
    }

    /**
     * Send the message to the coreBridge
     */
    public void sendThingFieldTickle(){
        start2();
    }

    private void start2()
    {
//        final String threadName = Thread.currentThread().getName();
//        Thread t = new Thread()
//        {
//            public void run()
//            {
                callback(false, Thread.currentThread().getName());
//            }
//        };

//        t.start();
    }

	public void sendThingFieldTickle(boolean publishTickleFlag){
		start2(publishTickleFlag);
	}

	private void start2( boolean publishTickleFlag)
	{
		callback(publishTickleFlag, Thread.currentThread().getName());

	}
}
