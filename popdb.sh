#!/bin/bash -x
#
# popdb.sh - primarily used by jenkins builds
#
# EXAMPLE: popdb.sh gradle-default.properties MojixRetail
#
#

# usage: popdb.sh gradle-default.properties MojixRetail

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

#MONGOHOST=$mongoHost
#MONGOPORT=$mongoPort
MONGOPRIMARY=$mongoPrimary
MONGODB=$mongoDb
MONGOUSERNAME=$mongoUsername
MONGOPASS=$mongoPassword
MONGOSSL=$mongoSSL
MONGOAUTHDB=$mongoAuthDB

POPDBNAME=$2

if [ "$3" = "clean" ]; then
    ./popdb_mongo.sh $MONGOPRIMARY $MONGOSSL $MONGODB $MONGOUSERNAME $MONGOPASS $MONGOAUTHDB

    mysqladmin -u $DBUSER -p$DBPASS -h 127.0.0.1 drop -f $DBNAME || true
    mysqladmin -u $DBUSER -p$DBPASS -h 127.0.0.1 create $DBNAME

    ./popdbappcore.sh $DBURL $DBUSER $DBPASS $DBDIALECT
    ./popdbiot.sh $DBURL $DBUSER $DBPASS $DBDIALECT $MONGOPRIMARY $MONGOSSL $MONGODB $MONGOAUTHDB
fi

if [ ! -z $POPDBNAME ]; then
 ./popdb-other.sh $DBURL $DBUSER $DBPASS $DBDIALECT $MONGOPRIMARY $MONGOSSL $MONGODB $MONGOAUTHDB $POPDBNAME
fi

