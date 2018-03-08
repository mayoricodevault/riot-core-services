@ECHO ON

SET SCRIPTPATH=%~dp0
SET CLASS=com.tierconnect.riot.iot.job.LoadZoneCountTable
SET FATJAR=riot-core-services-all-*.jar
SET JAVAPROPERTIES=-Dlog4j.configuration=log4j.stdout.properties

:: findJar sets the correct %CP%
CALL "%SCRIPTPATH%findJar.bat" %FATJAR%


java %JAVAPROPERTIES% -cp %CP% %CLASS% %1
