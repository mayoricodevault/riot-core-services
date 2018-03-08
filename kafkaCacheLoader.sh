#!/usr/bin/env bash
set -e

# Get the installer directory.
PROG=kafkaCacheLoader
SCRIPT_PATH=$(dirname `which $0`)

# Get the directory where the jar file resides
source $SCRIPT_PATH/functions.sh
CP=unknown
findFatJar

if [ ! -f $SCRIPT_PATH/${PROG}.conf ]; then
    echo "Could not find file: ${PROG}.conf"
    exit 1
fi

# Use the env variables for $PROG.
source $SCRIPT_PATH/${PROG}.conf

VIZIX_JAR_FILE=$SCRIPT_PATH/build/libs/riot-core-services-all.jar

# For MySQL host
if [ -n "${VIZIX_MYSQL_HOST+1}" ] && [ -n "${VIZIX_MYSQL_PASSWORD+1}" ] && [ -n "${VIZIX_MONGO_PRIMARY+1}" ]
then
  echo >&2 "Info: All environment variables were set properly."

    JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.driver_class=$VIZIX_MYSQL_DRIVER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://$VIZIX_MYSQL_HOST:3306/$VIZIX_MYSQL_DB"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$VIZIX_MYSQL_USER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$VIZIX_MYSQL_PASSWORD"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"
    JAVAPROPERTIES=$JAVAPROPERTIES" -D2f53086555b7fbb940ce78616ff212e5=false"

    JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.primary=$VIZIX_MONGO_PRIMARY"
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
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.zookeeper=$VIZIX_KAFKA_ZOOKEEPER"
    JAVAPROPERTIES=$JAVAPROPERTIES" -Dkafka.servers=$VIZIX_KAFKA_SERVERS"

    echo >&2 "Using this settings: $JAVAPROPERTIES"
    echo >&2 "Executing CacheLoaderTool"
    echo "java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderToolNew"
    java $JAVAPROPERTIES -cp $VIZIX_JAR_FILE com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderToolNew $@

    echo >&2 "Success! Kafka command was executed successfully"
    exit 0;
else
  echo >&2 "Error: Environment variables missing. Check that all required variables are being properly set. Exiting..."
  exit 1;
fi

