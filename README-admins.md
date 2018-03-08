QUICKSTART
----------

run as root user :

service tomcat stop
./popdb.sh $CONFIG_PROFILE $POPDB_DATASET #for new installs [ex. ./popdb.sh gradle-default.properties]
./install-war.sh
./install-mcb.sh
./install-alebridge.sh

/etc/rc.d/init.d/mcb start
/etc/rc.d/init.d/aleBridge start

tail -1000f /var/log/riot/mcb.log
tail -1000f /var/log/riot/aleBridge.log

