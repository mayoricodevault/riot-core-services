#!/bin/bash
#
# a debugging script for coreBridge long thing instantiate times issue
#
. functions.sh

CP=unknown

findFatJar #sets the correct $CP

JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"

if [ ! -z $1 ]; then
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://localhost:3306/riot_main"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=root"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=control123!"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.host="
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.port="
JAVAPROPERTIES=$JAVAPROPERTIES" -Dcassandra.keyspace="
fi

java $JAVAPROPERTIES -cp $CP com.tierconnect.riot.iot.popdb.PopDBBakuTags $@
