#!/bin/bash
# This script takes a cassandra and mysql database backups and created a backup_riotdb[TIMESTAMP].tar.gz
# Run "backup_db.sh -h" to see help.

CLIENT="client"
MYSQL_USER="root"
MYSQL_PASSWORD="control123!"
MYSQL_DB="riot_main"
BACKUP_DIR_PREFIX="backup_riotdb$CLIENT"
ENABLE_ATTACHMENTS_BACKUP="no"


#Getting the timestamp:
timestamp=`date +%Y%m%d%H%M%S`
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
mongodump --db riot_main --username admin --password control123! --authenticationDatabase admin --out mongodb\

#Attachments Backup
if [ $ENABLE_ATTACHMENTS_BACKUP = "yes" ]; then
mkdir attachments
cp /opt/$CLIENT_attachments/* attachments/
fi

cd ..
tar zcvf $BACKUP_DIR.tar.gz $BACKUP_DIR --remove-files
echo Backup ready: $BACKUP_DIR.tar.gz
