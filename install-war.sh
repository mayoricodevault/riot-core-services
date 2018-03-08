#!/bin/bash -x
#
#  install-war.sh
#

# Make sure only root can run our script
WAR_FILE=riot-core-services.war
XML_FILE=linux-riot-core-services.xml
MONGO_SCRIPT=disable-transparent-hugepages
LOGIO_SCRIPT=logio
VIZIX_SERVICES_DIR_PREFIX=$1

if [[ $EUID -ne 0 ]]; then
   echo "You are not root." 1>&2
fi

if [ ! -f $WAR_FILE ];
then
   echo "File $WAR_FILE does not exist - exiting..."
   exit 1
fi

if [ ! -f $XML_FILE ];
then
   echo "File $XML_FILE does not exist - exiting..."
   exit 1
fi

if [ -f /etc/rc.d/init.d/tomcat ];
then
TOMCAT_SERVICE=tomcat
else
TOMCAT_SERVICE=tomcat7
fi

mkdir -p $VIZIX_SERVICES_DIR_PREFIX/usr/local/riot/services
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/usr/local/riot/services/riot-core-services
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/var/lib/tomcat/webapps
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/etc/tomcat/Catalina/localhost
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/usr/local/riot/services/extra
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/etc/init.d
mkdir -p $VIZIX_SERVICES_DIR_PREFIX/etc/rc.d/init.d

RIOT_DIR=$VIZIX_SERVICES_DIR_PREFIX/usr/local/riot/services
WEBAPPS_DIR=$VIZIX_SERVICES_DIR_PREFIX/var/lib/tomcat/webapps
WAR_DIR=$VIZIX_SERVICES_DIR_PREFIX/usr/local/riot/services/riot-core-services
XML_DIR=$VIZIX_SERVICES_DIR_PREFIX/etc/tomcat/Catalina/localhost
WORK_DIR=$VIZIX_SERVICES_DIR_PREFIX/var/cache/tomcat/work
WORK_DIR2=$VIZIX_SERVICES_DIR_PREFIX/usr/share/tomcat/work
SCRIPTS_DIR=$VIZIX_SERVICES_DIR_PREFIX/etc/init.d
SCRIPTS_DIR_MAC=$VIZIX_SERVICES_DIR_PREFIX/etc/rc.d/init.d

rm -rf $WEBAPPS_DIR/riot-core-services/
rm -rf $WEBAPPS_DIR/riot-core-services.war
rm -rf $WORK_DIR/Catalina/localhost/riot-core-*
rm -rf $WORK_DIR2/Catalina/localhost/riot-core-*
rm -rf $XML_DIR/riot-core-services.xml

cp -f $WAR_FILE $WAR_DIR
cp -f $XML_FILE $XML_DIR
mv $XML_DIR/$XML_FILE $XML_DIR/riot-core-services.xml
cp *.sh *.jar *.xml *.md *.txt *.properties $RIOT_DIR
cp $MONGO_SCRIPT $SCRIPTS_DIR
cp $MONGO_SCRIPT $SCRIPTS_DIR_MAC
cp $MONGO_SCRIPT $RIOT_DIR/extra
cp $LOGIO_SCRIPT $RIOT_DIR/extra


if [ -e "/usr/bin/log.io-server" ]; then
    cp $LOGIO_SCRIPT $SCRIPTS_DIR
    cp $LOGIO_SCRIPT $SCRIPTS_DIR_MAC
fi
cd $WAR_DIR
jar -xvf riot-core-services.war

#echo "It's recommended to wait 60 seconds to give Tomcat time to initialize the webapp"
