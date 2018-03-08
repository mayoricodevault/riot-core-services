#!/bin/bash

DIR=$1

JAR=./build/libs/riot-core-services-all-dev.jar
PROG=com.tierconnect.riot.datagen.Plot

java -cp $JAR $PROG -dir $DIR -maxx $2 -maxy $3

PWD=`pwd`

cd $DIR 

gnuplot plot.def > plot.png

open plot.png 

cd $PWD
