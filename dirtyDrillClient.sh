#!/bin/bash

JAR="build/libs/riot-core-services-all-dev.jar"

DRILL="/usr/local/drill/jars/jdbc-driver/drill-jdbc-all-1.5.0.jar"

CP="-cp $JAR:$DRILL"

PROG="com.tierconnect.riot.datagen.DirtyDrillClient"

#ARGS="-dr jdbc:drill:zk=127.0.0.1:2181/drill/drillbits1;schema=mongo"

java $CP $PROPS $PROG "$@" 

