package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.List;

/**
 * Created by vramos on 8/23/16.
 */
public class KafkaConfig implements Serializable
{
	// these come from the Bridge config
	public boolean active = false;
	public String connectionCode = "KAFKA";
	
	// these come from the connection config
	// onle one of zookeeper or server will be defined
	// the kafka clients uses the zookeeper
	// the sprak-kafka client (v0.10) uses the kafka server
	public String zookeeper = "localhost:2181";
	public String server = "localhost:9092";
	public boolean checkpoint=false;
	public String consumerGroup;
	public List<String> topics;

	// Kafka streams application

	// Kafka configurations
	public String applicationId;
	public String stateStoreDirectory;
	public String numStreamThreads;
	public String batchSize;
	public String lingerMs;


	/**
	 * This method finds in topics a specify topic.
	 * @param topic e.g ___v1___data1
	 * @return
	 */
	public boolean containTopic(final String topic){
		Preconditions.checkNotNull(this.topics, "The list of topics is null");
		boolean isValidTopic = false;
		String topicFound=this.topics.stream().filter(s -> s.split(",")[0].equals(topic)).findAny().orElse(null);
		if (topicFound!=null){
			isValidTopic = true;
		}
		return isValidTopic;
	}

}
