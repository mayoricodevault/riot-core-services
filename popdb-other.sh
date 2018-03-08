#!/bin/bash
#
# Usage: popdb-more.sh MojixDemo
#
. functions.sh

CP=unknown

findFatJar #sets the correct $CP

JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"

if [ ! -z $1 ]; then
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=$1"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=$2"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=$3"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=$4"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.primary=$5"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.ssl=$6"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.db=$7"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dmongo.authdb=$8"
JAVAPROPERTIES=$JAVAPROPERTIES" -D2f53086555b7fbb940ce78616ff212e5=false"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dpopdb.option=$9"
fi

echo "Executing PopDB for $9"
java $JAVAPROPERTIES -cp $CP com.tierconnect.riot.iot.popdb.PopDBBase
