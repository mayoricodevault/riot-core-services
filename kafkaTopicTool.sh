#!/usr/bin/env bash
set -e

if [ -z $VIZIX_HOME_SERVICES ] ; then
   echo "Please define \$VIZIX_HOME_SERVICES"
   exit -1
fi

CP=$VIZIX_HOME_SERVICES/libs/riot-core-services-all.jar
PROG="com.tierconnect.riot.commons.kafka.topicTool.KafkaTopicTool"
JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"

CMD="java $JAVAPROPERTIES -cp $CP $PROG $@"
echo $CMD
sleep 1
$CMD
