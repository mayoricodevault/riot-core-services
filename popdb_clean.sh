#!/bin/bash -x
#
# popdb.sh - primarily used by jenkens builds
#
# EXAMPLE: popdb.sh gradle-default.properties
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
DBHOST=$dbHost
DBPORT=$dbPort

MONGOPRIMARY=$mongoPrimary
MONGOSSL=$mongoSSL
MONGODB=$mongoDb
MONGOUSERNAME=$mongoUsername
MONGOPASSWORD=$mongoPassword
MONGOAUTHDB=$mongoAuthDB

./popdb_mongo.sh $MONGOPRIMARY $MONGOSSL $MONGODB $MONGOUSERNAME $MONGOPASSWORD $MONGOAUTHDB

#./drop_keyspace.sh $CASSANDRAKEYSPACE
#./create_keyspace.sh $CASSANDRAKEYSPACE
#./create_tables.sh $CASSANDRAKEYSPACE

mysqladmin -h$DBHOST -P$DBPORT -u$DBUSER -p$DBPASS drop -f $DBNAME || true
mysqladmin -h$DBHOST -P$DBPORT -u$DBUSER -p$DBPASS create $DBNAME

