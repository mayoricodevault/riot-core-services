#!/bin/bash -x

rm -rf /usr/local/riot
rm -rf /var/run/riot
rm -rf /var/log/riot

rm -f /etc/rc.d/init.d/mcb
rm -f /etc/rc.d/init.d/mojixBridge #obsolete name
rm -f /etc/rc.d/init.d/aleBridge

rm -f /var/lib/tomcat/webapps/riot-core-services.war
rm -rf /var/lib/tomcat/webapps/riot-core-services
