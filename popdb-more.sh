#!/bin/bash
#
# EXAMPLE: popdb-more.sh gradle-default.properties MojixRetail
#
#

# usage: popdb.sh gradle-default.properties MojixRetail

if [ ! -f $1 ]; then
	echo "Can not find $1"
	exit
fi

source $1

JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"

if [ ! -z $1 ]; then
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=$hibernateConnectionUrl"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$hibernateConnectionUsername"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$hibernateConnectionPassword"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=$hibernateDialect"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.host=$mongoHost"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.port=$mongoPort"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.keyspace=$mongoDb"
fi

. functions.sh

CP=unknown

findFatJar #sets the correct $CP

POPDBNAME=$2

if [ ! -z $POPDBNAME ]; then
 java -cp $CP $JAVAPROPERTIES com.tierconnect.riot.iot.popdb.PopDB$2 $3 $4 $5 $6 $7
fi

