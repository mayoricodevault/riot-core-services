#!/bin/bash
#
# Functions used acrossed the scripts.
#
SCRIPT_PATH=$(dirname `which $0`)

findFatJar(){
  if [ -e $SCRIPT_PATH/build/libs/riot-core-services-all.jar ]; then
    CP=$SCRIPT_PATH/build/libs/riot-core-services-all.jar
  elif [ -e $SCRIPT_PATH/riot-core-services-all.jar ]; then
    CP=$SCRIPT_PATH/riot-core-services-all.jar
  elif [ -e /usr/local/riot/riot-core-services-all.jar ]; then
    CP=/usr/local/riot/riot-core-services-all.jar
  else
    echo "Could not find fat jar !"
    exit 1
  fi
  echo "INFO: findFatJar() Using fatJar: $CP"
}
