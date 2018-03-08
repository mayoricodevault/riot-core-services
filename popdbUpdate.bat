@ECHO ON
:: popdbUpdate.bat - Updates the databases (MySQL).
::
:: EXAMPLE:
:: popdbUpdate.bat DBHOST DBPORT DBNAME DBUSER DBPASS CASSANDRAHOST CASSANDRAPORT CASSANDRAKEYSPACE  FILESDIR
:: meaning:
:: popdbUpdate.bat localhost 3306 riot_main root control123! 127.0.0.1 9042 riot_main D:\bakuImport\BakuData
:: - loads configuration details from MojixRetail, optional argument defines which dataset to run (if any)

SET SCRIPTPATH=%~dp0

IF ["%1"]==[] (
	echo "Called without any arguments - exiting."
	exit /B 1
)

:: Load configuration variables from file specified (command line first argument)
cd %SCRIPTPATH%

SET DBHOST=%1
SET DBPORT=%2
SET DBNAME=%3
SET DBURL=jdbc:mysql://%1:%2/%3
SET DBUSER=%4
SET DBPASS=%5
SET CASSANDRAHOST=%6
SET CASSANDRAPORT=%7
SET CASSANDRAKEYSPACE=%8
SET FILESDIR=%9
SET DBDIALECT="org.hibernate.dialect.MySQLDialect"

SET CLASS=com.tierconnect.riot.iot.popdb.PopDBUpdate
SET FATJAR=riot-core-services-all-*.jar
SET JAVAPROPERTIES=-Dlog4j.configuration=log4j.stdout.properties

:: findJar sets the correct %CP%
CALL "%SCRIPTPATH%findJar.bat" %FATJAR%

IF NOT [%1]==[] (
SET JAVAPROPERTIES=%JAVAPROPERTIES% -Dhibernate.connection.url=%DBURL% -Dhibernate.connection.username=%DBUSER% -Dhibernate.connection.password=%DBPASS%  -Dhibernate.dialect=%DBDIALECT% -Dcassandra.host=%CASSANDRAHOST% -Dcassandra.port=%CASSANDRAPORT% -Dcassandra.keyspace=%CASSANDRAKEYSPACE% -Dfiles.dir=%FILESDIR%
)
::echo java %JAVAPROPERTIES% -cp %CP% %CLASS% %*
java %JAVAPROPERTIES% -cp %CP% %CLASS% %*
