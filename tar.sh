#!/bin/bash -x

NAME=riot-core-services #name of the inner directory

rm -rf build/tar

mkdir -p build/tar/$NAME

cp build/libs/* build/tar/$NAME
cp gradle-default.properties build/tar/$NAME
cp build/resources/main/*.sh build/tar/$NAME
cp build/resources/main/cassandra_db_init.cql build/tar/$NAME
cp README-admins.md build/tar/$NAME
cp src/main/resources/tomcat/riot-core-services.xml build/tar/$NAME

#scripts for Linux/MacOs
cp *.sh build/tar/$NAME/
chmod a+x build/tar/$NAME/*.sh
cp logio build/tar/$NAME/
cp disable-transparent-hugepages build/tar/$NAME/

#scripts for Windows
cp *.bat build/tar/$NAME/

#txt file for Branch
cp build/resources/main/*.txt build/tar/$NAME/

#linux xml file 
cp linux-riot-core-services.xml build/tar/$NAME/

#README file
cp README-dist.md build/tar/$NAME/

# creating .tar.gz
cd build/tar
tar -zcvf $NAME.tar.gz $NAME
