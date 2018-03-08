#!/bin/bash

#
# popdb_mongo.sh - Mkes mongo database
#
# Usage: popdb_mongo.bat MONGOPRIMARY MONGOSECONDARY MONGODB MONGOUSERNAME MONGOPASSWORD MONGOAUTHDB
#
# EXAMPLE: popdb.sh localhost:27017 false riot_main username password admin
#

MONGOPRIMARY=$1
MONGOSSL=$2
MONGODB=$3
MONGOUSERNAME=$4
MONGOPASSWORD=$5
MONGOAUTHDB=$6

MONGOSSLCOMMAND=""
if [ $MONGOSSL = "true" ]; then
    MONGOSSLCOMMAND="--ssl"
fi

isAuth=`mongo --eval "db.getUsers()" $1 | grep "not auth"`

if [ -z "$isAuth" ] ;
then
   echo "Mongo Authentication not enabled in host, skipping mongo nuke"
   exit 1
else
   echo "Mongo Authentication enabled in host"
   mongo $MONGOPRIMARY/$MONGODB -u $MONGOUSERNAME -p $MONGOPASSWORD $MONGOSSLCOMMAND --authenticationDatabase $MONGOAUTHDB --eval "db.getCollectionNames().forEach(function(c) { if (c.indexOf(\"system.\") == -1) db[c].drop(); })"
   mongo $MONGOPRIMARY/$MONGODB -u $MONGOUSERNAME -p $MONGOPASSWORD $MONGOSSLCOMMAND --authenticationDatabase $MONGOAUTHDB --eval "db.system.js.remove({_id : /^vizixFunction/}); db.system.js.remove({_id: \"JSONFormatter\"});"
   exit 0
fi