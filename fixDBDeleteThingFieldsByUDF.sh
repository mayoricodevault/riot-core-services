#!/bin/bash

. functions.sh

CP=unknown
findFatJar #sets the correct $CP
J1=com.tierconnect.riot.iot.fixdb.FixDBDeleteThingFieldsByUDF
JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"

java $JAVAPROPERTIES -cp $CP $J1 $@ 
