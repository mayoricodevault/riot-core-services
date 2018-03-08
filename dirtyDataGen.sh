#!/bin/bash

JAR="build/libs/riot-core-services-all-dev.jar"

CP="-cp $JAR"

PROG=" com.tierconnect.riot.datagen.DirtyDataGen"

java $CP $PROG "$@"


