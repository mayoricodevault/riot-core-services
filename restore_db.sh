#!/bin/bash

#usage: restore_db.sh CLIENT DBNAME

CLIENT=$1

DBNAME=$2



echo Connecting to S3 $BUCKET/$CLIENT/ and downloading  $DBNAME.tar.gz

s3cmd get s3://vizixbackups/$CLIENT/$DBNAME.tar.gz

echo Extracting

tar zxvf $DBNAME.tar.gz

cd $DBNAME

echo Restoring MySQL

mysql -uroot -pcontrol123! riot_main < mysql/mysqldump*.sql

echo Dropping MongoDB

mongo riot_main --eval "db.dropDatabase()"

echo Restoring MongoDB

mongorestore mongodb/
