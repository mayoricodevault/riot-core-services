#!/bin/bash
# This script takes a cassandra and mysql database backups and created a backup_riotdb[TIMESTAMP].tar.gz
# Run "backup_db.sh -h" to see help.
MYSQL_USER="root"
MYSQL_PASSWORD="control123!"
MYSQL_DB="riot_main"
BACKUP_DIR_PREFIX="backup_riotdb"
CLIENT="client"
#Getting the timestamp:
timestamp=`date +%Y%m%d`
#echo $timestamp
BACKUP_DIR=$BACKUP_DIR_PREFIX$timestamp


mkdir -p $BACKUP_DIR
cd $BACKUP_DIR

#MySQL Backup
echo Taking MySQL backup...
mkdir mysql
mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD --add-drop-database --add-drop-table --hex-blob --databases $MYSQL_DB > mysql/mysqldump$timestamp.sql

#Mongo Backup
echo Taking Mongo backup...
mkdir mongodb
mongodump --out mongodb/
cd ..
tar zcvf $BACKUP_DIR.tar.gz $BACKUP_DIR --remove-files
s3cmd put $BACKUP_DIR.tar.gz s3://vizixbackups/$CLIENT/
echo Backup ready in S3 Amazon Bucket
mkdir -p /var/lib/riot/backups
mv -f $BACKUP_DIR.tar.gz /var/lib/riot/backups/
echo Backup ready in folder /var/lib/riot/: $BACKUP_DIR.tar.gz
