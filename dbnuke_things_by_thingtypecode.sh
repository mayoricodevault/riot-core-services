#!/bin/bash
#
# dbnuke_things_by_thingtypecode.sh - Deletes things from specific thingTypeCode
#
# EXAMPLE: dbnuke_things_by_thingtypecode.sh localhost localhost default_rfid_thingtype
#
# usage: ./dbnuke_things_by_thingtypecode.sh localhost localhost default_rfid_thingtype

#if [ ! -f $1 ]; then
#	echo "usage: ./dbnuke_things_by_thingtypecode.sh localhost localhost default_rfid_thingtype"
#	exit
#fi

mysqlHost=$1
mongoHost=$2
thingType=$3

mysql_user=root
mysql_password=control123!

mongo_user=admin
mongo_password=control123!

if mysql -V > /dev/null; then
    if mongo --version > /dev/null; then
        echo Deleting MySQL
        #TODO improve query to support parent/child relationship
        mysql -h $mysqlHost -u $mysql_user -p$mysql_password riot_main -e "DELETE FROM apc_thing WHERE thingType_id=(SELECT id FROM thingtype WHERE thingTypeCode='$thingType');" -vvv

        echo Deleting Mongo
        #TODO improve query to support parent/child and thingTypeUDF relationships
        mongo $mongoHost/riot_main -u $mongo_user -p $mongo_password -authenticationDatabase admin --eval "var ids = db.things.find({\"thingTypeCode\": \"$thingType\"})
                                                                                                                              .map(function(x){return x._id});
                                                                                                           db.thingSnapshotIds.remove({\"_id\" : {\$in : ids}});
                                                                                                           db.thingSnapshots.remove({\"value._id\" : {\$in : ids}});
                                                                                                           db.things.remove({\"_id\" : {\$in : ids}});";
#        echo Repairing Mongo
#        mongo $mongoHost/riot_main -u $mongo_user -p $mongo_password -authenticationDatabase admin --eval "db.repairDatabase()";
    else
        echo Mongo not installed.
    fi
else
    echo MySQL not installed.
fi