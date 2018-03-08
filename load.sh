#!/bin/bash -x

#
# a simple DEV script to help re-load a database
#

CASSANDRAKEYSPACE=riot_main

./drop_keyspace.sh $CASSANDRAKEYSPACE
./create_keyspace.sh $CASSANDRAKEYSPACE
./create_tables.sh $CASSANDRAKEYSPACE

mysqladmin -u root -pcontrol123! drop -f riot_main
mysqladmin -u root -pcontrol123! create riot_main
mysql -u root -pcontrol123! riot_main < $1 


