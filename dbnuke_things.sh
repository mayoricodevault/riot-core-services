#!/bin/bash
# Use this script to remove all things from the RIoT database.
# Remember to stop Tomcat running RIoT Services before running this script.
# usage:
# dbnuke_things.sh coreBridge
# dbnuke_things.sh mysql
# dbnuke_things.sh mongo


db_type=$1

#cassandra_keyspace=riot_main

mysql_user=root
mysql_password=control123!

mssql_user=root
mssql_password=control123!

case "CASE_$db_type" in
    CASE_mysql)
	echo Trying MySQL:
        if mysql -V > /dev/null; then
            echo OK
            mysql -u $mysql_user -p$mysql_password -h 127.0.0.1 -e "SET FOREIGN_KEY_CHECKS = 0; TRUNCATE apc_thing; TRUNCATE thingImage; TRUNCATE ThingParentHistory; SET FOREIGN_KEY_CHECKS = 1;" riot_main
        else
            echo mysql not detected.
        fi
        ;;
    CASE_mongo)
	echo Trying Mongo:
        if mongo --version > /dev/null; then
            echo OK
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('things').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('thingSnapshotIds').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('thingSnapshots').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('timeseries').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('timeseriesControl').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.getCollection('exit_report').remove({})";
            mongo --port 27017 -u "admin" -p "control123!" --authenticationDatabase "admin" riot_main --eval "db.repairDatabase()";
        else
            echo mongo not detected.
        fi
        ;;
    CASE_coreBridge)
	echo Trying to delete coreBridge cache:
        rm -f /cache-thing*.*
        rm -f /root/cache-thing*.*
        rm -f /root/riot-core-bridges/cache-thing*.*
        ;;
    *)
        echo Please call with one parameter: mongo, mysql or coreBridge
esac
cd /
rm -f cache-thing*.*

