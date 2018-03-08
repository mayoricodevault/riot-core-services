#!/bin/sh
set -e

# Parameters
# VIZIX_MYSQL_HOST: The url or ip for MySQL. *required*
# VIZIX_MYSQL_DB: The database where ViZix is installed. Defaults to riot_main
# VIZIX_MYSQL_USER: The user to connect to MySQL. Defaults to root
# VIZIX_MYSQL_PASSWORD: The password to connect to MySQL. *required*
#
# VIZIX_MQTT_HOST: The url for the mosquitto server. *required*
# VIZIX_MQTT_PORT: MQTT port. Defaults to 1883
#
# VIZIX_BROKER_CONNECTION_WAIT: Defines if services will wait for broker connections
#
# VIZIX_MONGO_PRIMARY: MongoDB primary host url and port. *required*
# VIZIX_MONGO_SECONDARY: MongoDB replica host url and port.
# VIZIX_MONGO_REPLICASET: MongoDB replica set name.
# VIZIX_MONGO_SSL: Defines if MongoDB must use SSL for connection. *required*
# VIZIX_MONGO_USERNAME: MongoDB username used to connect database. *required*
# VIZIX_MONGO_PASSWORD: MongoDB password used to connect database. *required*
# VIZIX_MONGO_AUTHDB: MongoDB database used to authenticate connection. *required*
# VIZIX_MONGO_DB: Database for ViZix on MongoDB. Defaults to riot_main.
# VIZIX_MONGO_CONTROL_READPREFERENCE: MongoDB Read Preference for Control Module. Defaults to primary.
# VIZIX_MONGO_REPORTS_READPREFERENCE: MongoDB Read Preference for Reprots Module. Defaults to secondary.
# VIZIX_MONGO_SHARDING: Defines if the environment is sharded. *required*
# VIZIX_MONGO_CONNECTION_TIMEOUT: Max time before connection timeout. Defaults to 3000.
# VIZIX_MONGO_MAX_POOL_SIZE: Max pool size for connections. Defaults to 100.
#
# VIZIX_SPARK_ENABLED: Writes spark blink data into mongo (Blinked and Changed). Default is false.
# VIZIX_KAFKA_ENABLED: flag to enable necessary data for kafka core bridge in the popdb. Default is false.
#
# VIZIX_HAZELCAST_DISTRIBUTED_ENABLE: To allow services to work with a hazelcast cluster. By default in cloud its true
# VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS: The hazelcast server address. Defaults to localhost
# VIZIX_EMAIL_FAILURE: Mail recipients in the case of error sending emails.
# VIZIX_HAZELCAST_MC_ENABLE: to enable the management center monitor
# VIZIX_HAZELCAST_MC_URL: to specify the url of the management center
# VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE: to enable multicast on service cache

# Paths
tomcat_config_file='/usr/local/tomcat/conf/Catalina/localhost/riot-core-services.xml'
tomcat_config_file_original='/usr/local/tomcat/conf/Catalina/localhost/riot-core-services.xml.original'
tomcat_context_file=''

# 1. Setting defaults if not set via environment variables
VIZIX_MYSQL_DB=${VIZIX_MYSQL_DB:='riot_main'}
VIZIX_MYSQL_USER=${VIZIX_MYSQL_USER:='root'}
VIZIX_MYSQL_PASSWORD=${VIZIX_MYSQL_PASSWORD:='control123!'}
VIZIX_MYSQL_DRIVER=${VIZIX_MYSQL_DRIVER:='com.mysql.jdbc.Driver'}
VIZIX_HAZELCAST_HOST=${VIZIX_HAZELCAST_HOST:='services'}

VIZIX_MQTT_HOST=${VIZIX_MQTT_HOST:=mosquitto}
VIZIX_MQTT_PORT=${VIZIX_MQTT_PORT:=1883}

VIZIX_BROKER_CONNECTION_WAIT=${VIZIX_BROKER_CONNECTION_WAIT:=false}

VIZIX_MONGO_PRIMARY=${VIZIX_MONGO_PRIMARY:='mongo:27017'}
VIZIX_MONGO_SECONDARY=${VIZIX_MONGO_SECONDARY:=''}
VIZIX_MONGO_REPLICASET=${VIZIX_MONGO_REPLICASET:=''}
VIZIX_MONGO_SSL=${VIZIX_MONGO_SSL:='false'}
VIZIX_MONGO_USERNAME=${VIZIX_MONGO_USERNAME:='admin'}
VIZIX_MONGO_PASSWORD=${VIZIX_MONGO_PASSWORD:='control123!'}
VIZIX_MONGO_AUTHDB=${VIZIX_MONGO_AUTHDB:='admin'}
VIZIX_MONGO_DB=${VIZIX_MONGO_DB:='riot_main'}
VIZIX_MONGO_CONTROL_READPREFERENCE=${VIZIX_MONGO_CONTROL_READPREFERENCE:='primary'}
VIZIX_MONGO_REPORTS_READPREFERENCE=${VIZIX_MONGO_REPORTS_READPREFERENCE:='secondary'}
VIZIX_MONGO_SHARDING=${VIZIX_MONGO_SHARDING:='false'}
VIZIX_MONGO_CONNECTION_TIMEOUT=${VIZIX_MONGO_CONNECTION_TIMEOUT:=0}
VIZIX_MONGO_MAX_POOL_SIZE=${VIZIX_MONGO_MAX_POOL_SIZE:=0}

VIZIX_SPARK_ENABLED=${VIZIX_SPARK_ENABLED:='false'}

VIZIX_KAFKA_ENABLED=${VIZIX_KAFKA_ENABLED:='false'}
VIZIX_KAFKA_ZOOKEEPER=${VIZIX_KAFKA_ZOOKEEPER:='localhost:2181'}
VIZIX_KAFKA_SERVERS=${VIZIX_KAFKA_SERVERS:='localhost:9092'}

VIZIX_HAZELCAST_DISTRIBUTED_ENABLE=${VIZIX_HAZELCAST_DISTRIBUTED_ENABLE:='true'}
VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS=${VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS:='localhost'}
VIZIX_HAZELCAST_MC_ENABLE=${VIZIX_HAZELCAST_MC_ENABLE:='false'}
VIZIX_HAZELCAST_MC_URL=${VIZIX_HAZELCAST_MC_URL:='http://localhost:8080/mancenter'}
VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE=${VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE:='false'}
VIZIX_AUTHENTICATION_MODE=${VIZIX_AUTHENTICATION_MODE:=''}

VIZIX_SCHEDULED_RULES_ENABLED=${VIZIX_SCHEDULED_RULES_ENABLED:='true'}

# Memory settings
MEM_XMX=${MEM_XMX:='1024m'}
MEM_XMS=${MEM_XMS:='512m'}

# JAR file location
VIZIX_JAR_FILE=${VIZIX_JAR_FILE:='/jar/*.jar'}
VIZIX_HOME_SERVICES='/jar'
# 2. Checking required variables and setting up xml configuration file

# For MySQL host
if [ -n "${VIZIX_MYSQL_HOST+1}" ] && [ -n "${VIZIX_MYSQL_PASSWORD+1}" ] && [ -n "${VIZIX_MQTT_HOST+1}" ] && [ -n "${VIZIX_MONGO_PRIMARY+1}" ]
then
  echo >&2 "Info: All environment variables were set properly."

  # 2A. INSTALLATION: If the script contains the install parameter, execute the installation. E.g. /run.sh install
  if [ "$1" = "install" ]; then
    echo >&2 "Configuring installation..."
    JAVAPROPERTIES=$JAVAPROPERTIES" -Duser.timezone=UTC"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dlog4j.configuration=log4j.stdout.properties"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.driver_class=$VIZIX_MYSQL_DRIVER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://$VIZIX_MYSQL_HOST:3306/$VIZIX_MYSQL_DB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$VIZIX_MYSQL_USER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$VIZIX_MYSQL_PASSWORD"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.cache.hazelcast.native_client_address=$VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS"
    JAVAPROPERTIES=$JAVAPROPERTIES" -D2f53086555b7fbb940ce78616ff212e5=false"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dauthentication.mode=$VIZIX_AUTHENTICATION_MODE"

    if [ "$3" = "clean" ]; then
        echo >&2 "Using this settings: $JAVAPROPERTIES"

        echo >&2 "Executing PopDBRequired"
        java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.appcore.popdb.PopDBRequired
    fi

    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.primary=$VIZIX_MONGO_PRIMARY"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.secondary=$VIZIX_MONGO_SECONDARY"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.replicaset=$VIZIX_MONGO_REPLICASET"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.ssl=$VIZIX_MONGO_SSL"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.username=$VIZIX_MONGO_USERNAME"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.password=$VIZIX_MONGO_PASSWORD"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.authdb=$VIZIX_MONGO_AUTHDB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.db=$VIZIX_MONGO_DB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.controlReadPreference=$VIZIX_MONGO_CONTROL_READPREFERENCE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.reportsReadPreference=$VIZIX_MONGO_REPORTS_READPREFERENCE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.sharding=$VIZIX_MONGO_SHARDING"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.connectiontimeout=$VIZIX_MONGO_CONNECTION_TIMEOUT"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.maxpoolsize=$VIZIX_MONGO_MAX_POOL_SIZE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmqtt.host=$VIZIX_MQTT_HOST"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmqtt.port=$VIZIX_MQTT_PORT"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dspark.enabled=$VIZIX_SPARK_ENABLED"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.enabled=$VIZIX_KAFKA_ENABLED"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.zookeeper=$VIZIX_KAFKA_ZOOKEEPER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.servers=$VIZIX_KAFKA_SERVERS"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dscheduledrule.enabled=$VIZIX_SCHEDULED_RULES_ENABLED"

    # popDB options are:
    # AppRetail -> Current MojixRetail
    # AppRetailKafka -> MojixRetail+Kafka
    # Automation -> Internal, used by QA
    # Blockchain -> MojixRetail+Kafka+Blockchain
    # CoreTenant -> Empty instalation
    # DemoRetailTasks -> Retail Thing types and reports
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dpopdb.option=$2"

    if [ "$3" = "clean" ]; then
        echo >&2 "Using this settings: $JAVAPROPERTIES"

        echo >&2 "Executing PopDBRequiredIOT"
        java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.popdb.PopDBRequiredIOT
    fi

    echo >&2 "Executing PopDB$2"
    java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.popdb.PopDBBase

    echo >&2 "Success! Installation executed successfully"
    exit 0;
  fi

  if [ "$1" = "kafka" ]; then
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dlog4j.configuration=log4j.stdout.properties"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.driver_class=$VIZIX_MYSQL_DRIVER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://$VIZIX_MYSQL_HOST:3306/$VIZIX_MYSQL_DB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$VIZIX_MYSQL_USER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$VIZIX_MYSQL_PASSWORD"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.cache.hazelcast.native_client_address=$VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS"
    JAVAPROPERTIES=$JAVAPROPERTIES" -D2f53086555b7fbb940ce78616ff212e5=false"

    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.primary=$VIZIX_MONGO_PRIMARY"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.secondary=$VIZIX_MONGO_SECONDARY"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.replicaset=$VIZIX_MONGO_REPLICASET"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.ssl=$VIZIX_MONGO_SSL"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.username=$VIZIX_MONGO_USERNAME"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.password=$VIZIX_MONGO_PASSWORD"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.authdb=$VIZIX_MONGO_AUTHDB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.db=$VIZIX_MONGO_DB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.controlReadPreference=$VIZIX_MONGO_CONTROL_READPREFERENCE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.reportsReadPreference=$VIZIX_MONGO_REPORTS_READPREFERENCE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.sharding=$VIZIX_MONGO_SHARDING"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.connectiontimeout=$VIZIX_MONGO_CONNECTION_TIMEOUT"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.maxpoolsize=$VIZIX_MONGO_MAX_POOL_SIZE"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmqtt.host=$VIZIX_MQTT_HOST"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmqtt.port=$VIZIX_MQTT_PORT"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dspark.enabled=$VIZIX_SPARK_ENABLED"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.enabled=$VIZIX_KAFKA_ENABLED"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.zookeeper=$VIZIX_KAFKA_ZOOKEEPER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.servers=$VIZIX_KAFKA_SERVERS"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dscheduledrule.enabled=$VIZIX_SCHEDULED_RULES_ENABLED"

    echo >&2 "Using this settings: $JAVAPROPERTIES"

    if [ "$2" = "createTopics" ]; then
      echo >&2 "Creating the topics"
    echo "java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.commons.kafka.topicTool.KafkaTopicTool -c"
      java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.commons.kafka.topicTool.KafkaTopicTool -z $VIZIX_KAFKA_ZOOKEEPER -c
    fi

    if [ "$2" = "loadCache" ]; then
      echo >&2 "Executing CacheLoaderTool"
      echo "java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderToolNew"
      java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderToolNew -k $VIZIX_KAFKA_SERVERS
    fi

    if [ "$2" = "popdb" ]; then
      echo >&2 "Executing PopDBKafka"
      echo "java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.popdb.PopDBKafka"
      java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.popdb.PopDBKafka
    fi

    echo >&2 "Success! Kafka command was executed successfully"
    exit 0;
  fi

  # 2B. Check if original riot-core-services.xml file exists
  if [ -f "$tomcat_config_file_original" ]
  then
    # file exists
    # remove the existing config.js file
    rm -f $tomcat_config_file
  else
    # file doesn't exist; this is the first boot
    # rename the file to config.js.original
    mv $tomcat_config_file $tomcat_config_file_original
  fi

  # copy the original config file
  cp $tomcat_config_file_original $tomcat_config_file
  # Replacing variables
  sed -i "s/VIZIX_MYSQL_DRIVER/$VIZIX_MYSQL_DRIVER/g" $tomcat_config_file
  sed -i "s/VIZIX_MYSQL_HOST/$VIZIX_MYSQL_HOST/g" $tomcat_config_file
  sed -i "s/VIZIX_MYSQL_PASSWORD/$VIZIX_MYSQL_PASSWORD/g" $tomcat_config_file
  sed -i "s/VIZIX_MYSQL_DB/$VIZIX_MYSQL_DB/g" $tomcat_config_file
  sed -i "s/VIZIX_MYSQL_USER/$VIZIX_MYSQL_USER/g" $tomcat_config_file
  sed -i "s/VIZIX_HAZELCAST_HOST/$VIZIX_HAZELCAST_HOST/g" $tomcat_config_file

  sed -i "s/VIZIX_MQTT_HOST/$VIZIX_MQTT_HOST/g" $tomcat_config_file
  sed -i "s/VIZIX_MQTT_PORT/$VIZIX_MQTT_PORT/g" $tomcat_config_file

  sed -i "s/VIZIX_BROKER_CONNECTION_WAIT/$VIZIX_BROKER_CONNECTION_WAIT/g" $tomcat_config_file

  sed -i "s/VIZIX_MONGO_PRIMARY/$VIZIX_MONGO_PRIMARY/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_SECONDARY/$VIZIX_MONGO_SECONDARY/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_REPLICASET/$VIZIX_MONGO_REPLICASET/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_SSL/$VIZIX_MONGO_SSL/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_USERNAME/$VIZIX_MONGO_USERNAME/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_PASSWORD/$VIZIX_MONGO_PASSWORD/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_AUTHDB/$VIZIX_MONGO_AUTHDB/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_DB/$VIZIX_MONGO_DB/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_CONTROL_READPREFERENCE/$VIZIX_MONGO_CONTROL_READPREFERENCE/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_REPORTS_READPREFERENCE/$VIZIX_MONGO_REPORTS_READPREFERENCE/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_SHARDING/$VIZIX_MONGO_SHARDING/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_CONNECTION_TIMEOUT/$VIZIX_MONGO_CONNECTION_TIMEOUT/g" $tomcat_config_file
  sed -i "s/VIZIX_MONGO_MAX_POOL_SIZE/$VIZIX_MONGO_MAX_POOL_SIZE/g" $tomcat_config_file

  sed -i "s/VIZIX_SPARK_ENABLED/$VIZIX_SPARK_ENABLED/g" $tomcat_config_file
  sed -i "s/VIZIX_KAFKA_ENABLED/$VIZIX_KAFKA_ENABLED/g" $tomcat_config_file
  sed -i "s/VIZIX_KAFKA_ZOOKEEPER/$VIZIX_KAFKA_ZOOKEEPER/g" $tomcat_config_file
  sed -i "s/VIZIX_KAFKA_SERVERS/$VIZIX_KAFKA_SERVERS/g" $tomcat_config_file
  sed -i "s/VIZIX_BROKER_OUTPUT_FORMAT/$VIZIX_BROKER_OUTPUT_FORMAT/g" $tomcat_config_file

  sed -i "s/VIZIX_HAZELCAST_DISTRIBUTED_ENABLE/$VIZIX_HAZELCAST_DISTRIBUTED_ENABLE/g" $tomcat_config_file
  sed -i "s/VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS/$VIZIX_HAZELCAST_NATIVE_CLIENT_ADDRESS/g" $tomcat_config_file
  #sed -i "s/VIZIX_HAZELCAST_MC_ENABLE/$VIZIX_HAZELCAST_MC_ENABLE/g" $tomcat_config_file
  #sed -i "s/VIZIX_HAZELCAST_MC_URL/$VIZIX_HAZELCAST_MC_URL/g" $tomcat_config_file
  #sed -i "s/VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE/VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE/g" $tomcat_config_file
  sed -i "s/VIZIX_EMAIL_FAILURE/$VIZIX_EMAIL_FAILURE/g" $tomcat_config_file
  sed -i "s/VIZIX_AUTHENTICATION_MODE/$VIZIX_AUTHENTICATION_MODE/g" $tomcat_config_file
  sed -i "s/VIZIX_SCHEDULED_RULES_ENABLED/$VIZIX_SCHEDULED_RULES_ENABLED/g" $tomcat_config_file
else
  echo >&2 "Error: Environment variables missing. Check that all required variables are being properly set. Exiting..."
  exit 1;
fi

# 3. Getting and setting CATALINA_OPTS settings
export CATALINA_OPTS="$CATALINA_OPTS -Xmx$MEM_XMX"
export CATALINA_OPTS="$CATALINA_OPTS -Xms$MEM_XMS"
export CATALINA_OPTS="$CATALINA_OPTS -Duser.timezone=UTC"
export CATALINA_OPTS="$CATALINA_OPTS -Djava.endorsed.dirs=$CATALINA_HOME/endorsed"
export CATALINA_OPTS="$CATALINA_OPTS -Dvizix.hazelcast.managementcenter.enable=$VIZIX_HAZELCAST_MC_ENABLE"
export CATALINA_OPTS="$CATALINA_OPTS -Dvizix.hazelcast.managementcenter.url=$VIZIX_HAZELCAST_MC_URL"
export CATALINA_OPTS="$CATALINA_OPTS -Dvizix.hazelcast.services.multicast.enable=$VIZIX_HAZELCAST_SERVICES_MULTICAST_ENABLE"


# export CATALINA_OPTS="$CATALINA_OPTS -server"
# export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=5120m"
# export CATALINA_OPTS="$CATALINA_OPTS -Xss1024k"
# export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseParallelGC"
# export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=1500"
# export CATALINA_OPTS="$CATALINA_OPTS -XX:GCTimeRatio=9"
# export CATALINA_OPTS="$CATALINA_OPTS -server"
# export CATALINA_OPTS="$CATALINA_OPTS -XX:+DisableExplicitGC"

# export JAVA_OPTS="$JAVA_OPTS -server"

echo "Using CATALINA_OPTS:"
for arg in $CATALINA_OPTS
do
    echo ">> " "$arg"
done
echo ""

echo "Using JAVA_OPTS:"
for arg in $JAVA_OPTS
do
    echo ">> " "$arg"
done
echo "_______________________________________________"
echo ""

# 4. Done! let's execute this app
echo >&2 "Application was successfully configured!"

exec "$@"
