#!/bin/bash -x
#
# EXAMPLE: popdb.sh gradle-default.properties /root/csvfiles/
#
# usage: popdbUpdate.sh PROPERTY_FILE CSF_FILES_DIR
. functions.sh


if [ ! -f $1 ]; then
	echo "Called without any arguments - exiting."
	exit
fi

source $1
DBURL=$hibernateConnectionUrl
DBUSER=$hibernateConnectionUsername
DBPASS=$hibernateConnectionPassword
DBNAME=$hibernateConnectionDBName
DBDIALECT=$hibernateDialect

CASSANDRAHOST=$cassandraHost
CASSANDRAPORT=$cassandraPort
CASSANDRAKEYSPACE=$cassandraKeyspace

FILESDIR=$2
CLASS="com.tierconnect.riot.iot.popdb.PopDBUpdate"

findFatJar #sets the correct $CP

JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"
if [ ! -z $1 ]; then
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=$DBURL"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$DBUSER"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$DBPASS"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=$DBDIALECT"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.host=$CASSANDRAHOST"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.port=$CASSANDRAPORT"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.keyspace=riot_main"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dfiles.dir=$FILESDIR"
fi

java $JAVAPROPERTIES -cp $CP $CLASS $@