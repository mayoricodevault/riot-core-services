#!/bin/bash -x
#
# popdb_incremental.sh - Populates new data without reseting the DB data
#
# EXAMPLE: popdb_incremental.sh gradle-default.properties ML
#
#

# usage: popdb_incremental.sh gradle-default.properties ML

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

