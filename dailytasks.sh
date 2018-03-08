#!/bin/bash
CASSANDRA_DATA_PATH="/var/lib/cassandra/data"
CASSANDRA_KEYSPACE="riot_main"
MYSQL_USER="root"
MYSQL_PASSWORD="control123!"
MYSQL_DB="riot_main"
BACKUP_DIR_PREFIX="riot_main"
REMOVE_OLD_SNAPSHOTS="false"
SERVICES_LOG_PATH="/usr/tomcat/apache-tomcat-7.0.62/logs/"
BRIDGES_LOG_PATH="/var/log/riot"
MYSQL_DB_NUKE="no"
CASSANDRA_DB_NUKE="no"
AFTER_NUKE="no"

###############
#Stopping Vizix
echo Stopping aleBridge...
#Stop aleBridge
service aleBridge stop
echo  Stopping coreBridge...
#Stop coreBridge
service coreBridge stop
echo Stopping Services...
#Stop Services
/usr/tomcat/apache-tomcat-7.0.62/bin/shutdown.sh
echo Stopping Mosquitto...
#Stop Mosquitto
service mosquitto stop
##########
#DB Backup
echo DB Backup...
#Getting the timestamp:
timestamp=`date +%Y%m%d%H%M%S`
#Creating BACKUP_DIR
BACKUP_DIR=$BACKUP_DIR_PREFIX$timestamp
mkdir -p $BACKUP_DIR
cd $BACKUP_DIR
#MySQL Dump
echo Taking MySQL backup...
mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD --add-drop-database --add-drop-table --hex-blob --databases $MYSQL_DB > mysqldump$MYSQL_DB_$timestamp.sql
#Cassandra Dump
echo Taking Cassandra backup...
nodetool cleanup
if [ $REMOVE_OLD_SNAPSHOTS = "true" ]; then
 nodetool clearsnapshot $CASSANDRA_KEYSPACE
fi
nodetool snapshot $CASSANDRA_KEYSPACE -t $CASSANDRA_KEYSPACE_$timestamp
echo Compressing Snapshots...
#Compressing Snapshots
tar zcvf field_type$timestamp.tar.gz $CASSANDRA_DATA_PATH/$CASSANDRA_KEYSPACE/field_type/snapshots/$CASSANDRA_KEYSPACE_$timestamp
tar zcvf field_value$timestamp.tar.gz $CASSANDRA_DATA_PATH/$CASSANDRA_KEYSPACE/field_value/snapshots/$CASSANDRA_KEYSPACE_$timestamp
tar zcvf field_value_history$timestamp.tar.gz $CASSANDRA_DATA_PATH/$CASSANDRA_KEYSPACE/field_value_history/snapshots/$CASSANDRA_KEYSPACE_$timestamp
tar zcvf mqtt_sequence_number$timestamp.tar.gz $CASSANDRA_DATA_PATH/$CASSANDRA_KEYSPACE/mqtt_sequence_number/snapshots/$CASSANDRA_KEYSPACE_$timestamp
tar zcvf zone_count$timestamp.tar.gz $CASSANDRA_DATA_PATH/$CASSANDRA_KEYSPACE/zone_count/snapshots/$CASSANDRA_KEYSPACE_$timestamp
cd ..
echo Compressing Logs...
#Compressing Logs 
tar zcvf serviceslog$timestamp.tar.gz $SERVICES_LOG_PATH
tar zcvf serviceslog$timestamp.tar.gz $BRIDGES_LOG_PATH
echo Compressing all files...
#Compressing all files
tar zcvf $BACKUP_DIR.tar.gz $BACKUP_DIR --remove-files
echo Backup ready: $BACKUP_DIR.tar.gz

########
#DB Nuke
if [$CASSANDRA_DB_NUKE = "yes"]; then
echo Nuking Cassandra...
cqlsh -k $CASSANDRA_KEYSPACE -e "TRUNCATE field_value; TRUNCATE field_value_history; TRUNCATE mqtt_sequence_number; TRUNCATE zone_count; TRUNCATE field_type;"
fi
if [$MYSQL_DB_NUKE = "yes"]; then 
echo Nuking MySQL...
mysql -u $MYSQL_USER -p$MYSQL_PASSWORD -e "SET FOREIGN_KEY_CHECKS = 0; TRUNCATE apc_thing; TRUNCATE thingfield; TRUNCATE thingfieldvalue; TRUNCATE thingimage; TRUNCATE thingparenthistory; SET FOREIGN_KEY_CHECKS = 1;" riot_main
fi
##########
#AfterNuke
echo Deleting Logs...
#Deteling Logs
rm -f /var/log/riot/*.*
rm -f /usr/tomcat/apache-tomcat-7.0.62/logs/*.*
echo deleting coreBridge cache...
#Delete coreBridge cache
cd /
rm -f cache-thing*.*
echo Deleting mosquitto db file...
#Delete mosquitto db file
cd /var/lib/mosquitto/
rm -f mosquitto.db

###############
#Starting Vizix
echo Starting Mosquitto
#Start Mosquitto
service mosquitto start
echo Starting Services
#Start Services
/usr/tomcat/apache-tomcat-7.0.62/bin/catalina.sh start
echo Starting coreBridge
#Start coreBridge
service coreBridge start
echo Starting aleBridge
#Start aleBridge
service aleBridge start
