#!/bin/bash -x
#
#  deploy.sh gradle-default.properties riot MojixRetail
#

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

if [ -f /etc/rc.d/init.d/tomcat7 ];
then
TOMCAT_SERVICE=tomcat7
else
TOMCAT_SERVICE=tomcat
fi

service $TOMCAT_SERVICE stop

CONFIG_PROFILE=$1
POPDB_DATASET=$2

./popdb.sh $CONFIG_PROFILE $POPDB_DATASET

./install-war.sh
