#!/bin/bash 

JAVAPROPERTIES="-Dlog4j.configuration=log4j.stdout.properties"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.url=jdbc:mysql://localhost:3306/riot_main"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.username=root"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.connection.password=control123!"
JAVAPROPERTIES=$JAVAPROPERTIES" -Dhibernate.dialect=org.hibernate.dialect.MySQLDialect"

java $JAVAPROPERTIES -cp build/libs/riot-core-services-all-3.1.0.jar  com.tierconnect.riot.iot.services.UnitService $@
