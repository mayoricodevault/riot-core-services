package com.tierconnect.riot.commons.utils;

import kafka.admin.AdminOperationException;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.log4j.Logger;

import java.util.Properties;

public class KafkaZkUtils {

    private static final Logger logger = Logger.getLogger( KafkaZkUtils.class );

    // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
    private String zookeeperHosts;


    private final int sessionTimeOutInMs = 15 * 1000; // 15 secs
    private final int connectionTimeOutInMs = 10 * 1000; // 10 secs

    public KafkaZkUtils(String zookeeperHosts) {
        this.zookeeperHosts = zookeeperHosts;

    }

    /**
     * This method create or add partitions and replications kafka
     * @param topicName
     * @param noOfPartitions
     * @param noOfReplication
     */
    public void createOrAddPartitionReplicas(final String topicName,final int noOfPartitions,final int noOfReplication){

        Properties topicConfiguration = new Properties();

        ZkClient zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
        ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);

        try{
            if (!AdminUtils.topicExists(zkUtils,topicName)){
                AdminUtils.createTopic(zkUtils, topicName, noOfPartitions, noOfReplication, topicConfiguration, RackAwareMode.Disabled$.MODULE$);
            }else {
                int noPartitionCurrent=getNumberOfPartitions(topicName);
                if (noOfPartitions != noPartitionCurrent){
                    if (noOfPartitions > noPartitionCurrent){
                        AdminUtils.addPartitions(zkUtils, topicName, noOfPartitions, "", true, AdminUtils.addPartitions$default$6());
                    }else {
                        logger.error("The number of partitions for a topic:"+topicName+" must be major to "+noPartitionCurrent);
                    }
                }
            }
        }catch (AdminOperationException aoex){
            logger.error("Occurred an error with kafka client.",aoex);
        }
        catch (Exception ex){
            logger.error("Occurred an error with kafka client.",ex);
        }finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
    }


    public int getNumberOfPartitions(String topic){
        ZkClient zkClient = null;
        ZkUtils zkUtils = null;
        int noPartitions=-1;
        try {
            zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
            zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);
            MetadataResponse.TopicMetadata metaData = AdminUtils.fetchTopicMetadataFromZk(topic,zkUtils);
            noPartitions=metaData.partitionMetadata().size();
            logger.info("Number of partitions:"+noPartitions);
        }catch (Exception ex){
            logger.error("Occurred an error with kafka client.",ex);
        }finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
        return noPartitions;

    }


    /**
     *
     * @param topicName
     */
    public void deleteTopic(String topicName) {

        ZkClient zkClient =null;
        ZkUtils zkUtils=null;
        try{
            zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
            zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);
            AdminUtils.deleteTopic(zkUtils, topicName);

            zkClient.deleteRecursive(ZkUtils.getTopicPath(topicName));
            zkClient.deleteRecursive(ZkUtils.getTopicPath(topicName));
        }catch (Exception ex){
            logger.error("Occurred an error with kafka client.",ex);
        }finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
    }


    /**
     *
     * @param topicName
     * @return boolean
     */
    public boolean existTopic(String topicName){
        boolean exist=false;
        ZkClient zkClient =null;
        ZkUtils zkUtils=null;
        try{
            zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
            zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);
            exist=AdminUtils.topicExists(zkUtils,topicName);
            return exist;
        }catch (Exception ex){
            logger.error("Occurred an error with kafka client.",ex);
        }finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
        return exist;
    }

    /**
     * Verify if kafka server is active.
     * @return boolean
     */
    public boolean isConnectionActive(){
        boolean isActive=false;
        ZkClient zkClient =null;
        ZkUtils zkUtils=null;
        try{
            zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
            zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);
            if (zkUtils.getAllBrokersInCluster().size()==0){
                isActive=false;
            }else {
                isActive=true;
            }
        }catch (Exception ex){
            isActive=false;
        }finally {
            if (zkClient != null) {
                zkClient.close();
            }
        }
        return isActive;
    }
}
