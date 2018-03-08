#!/bin/bash
#
# Usage: popdb-more.sh MojixDemo
#
if [ ! -f $1 ]; then
	echo "Can not find $1"
	exit
fi

source $1
DBURL=$hibernateConnectionUrl
DBUSER=$hibernateConnectionUsername
DBPASS=$hibernateConnectionPassword
DBNAME=$hibernateConnectionDBName
DBDIALECT=$hibernateDialect

MONGOHOST=$mongoHost
MONGOPORT=$mongoPort
MONGODB=$mongoDb
MONGOUSERNAME=$mongoUsername
MONGOPASS=$mongoPassword

POPDBNAME=$2


if [ ! -z $POPDBNAME ]; then
 ./popdb-other.sh $DBURL $DBUSER $DBPASS $DBDIALECT $MONGOHOST $MONGOPORT $MONGODB $POPDBNAME
fi