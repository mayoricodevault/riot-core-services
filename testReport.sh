#!/bin/bash 
#
# Usage: test.sh [report_id]
#

# ./test.sh jdbc:mysql://localhost:3306/riot_main root control123! org.hibernate.dialect.MySQLDialect


. functions.sh

CP=unknown

findFatJar #sets the correct $CP

JAVAPROPERTIES="-Dlog4j.configuration=log4j.reportTest.properties"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://localhost:3306/riot_main"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=root"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=control123!"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"

java $JAVAPROPERTIES -cp $CP com.tierconnect.riot.iot.reports_new.Test $@

