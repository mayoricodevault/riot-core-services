@ECHO ON
:: Usage: popdbappcore.bat dburl dbuser dbpass

ECHO "Executing popdbappcore.bat"

SET SCRIPTPATH=%~dp0
SET CLASS=com.tierconnect.riot.appcore.popdb.PopDBRequired
SET FATJAR=riot-core-services-all.jar
SET JAVAPROPERTIES=-Dlog4j.configuration=log4j.stdout.properties

:: findJar sets the correct %CP%
CALL "%SCRIPTPATH%findJar.bat" %FATJAR%

IF NOT [%1]==[] (
SET JAVAPROPERTIES=%JAVAPROPERTIES% -Dhibernate.connection.url=%1 -Dhibernate.connection.username=%2 -Dhibernate.connection.password=%3 -Dhibernate.dialect=%4 -D2f53086555b7fbb940ce78616ff212e5=false
)

java  %JAVAPROPERTIES% -cp %CP% %CLASS% %*
