#!/bin/bash -x
#
# Usage: popdbappcore.sh [localhost port dbname dbuser dbpass]
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
JAVAPROPERTIES=$JAVAPROPERTIES" -D2f53086555b7fbb940ce78616ff212e5=false"
fi

java $JAVAPROPERTIES -cp $CP com.tierconnect.riot.appcore.popdb.PopDBRequired $@
