@ECHO ON
:: popdb.bat - Populates the databases (MySQL + Cassandra).
::
:: EXAMPLE:
:: popdb_clean.bat MSSQLCONN MONGOCONN
:: meaning:
:: popdb.bat localhost 3306 riot_main root control123! localhost 27017 riot_main MojixRetail
:: - loads configuration details from MojixRetail, optional argument defines which dataset to run (if any)

SET SCRIPTPATH=%~dp0

IF ["%1"]==[] (
	echo "Called without any arguments - exiting."
	exit /B 1
)

:: Load configuration variables from file specified (command line first argument)
cd %SCRIPTPATH%


SET PREFIXMYSQL=MYSQL
SET PREFIXMONGO=MONGO
SET MYSQLDIALECT="org.hibernate.dialect.MySQLDialect"

call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMYSQL% %1
call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMONGO% %2

:: call "%SCRIPTPATH%popdb-cassandra.bat" %CASSANDRAKEYSPACE% %CASSANDRAHOST%

call "%SCRIPTPATH%popdb_mongo.bat" %2

mysqladmin -h%MYSQLHOST% -P%MYSQLPORT% -u%MYSQLUSERNAME% -p%MYSQLPASSWORD% drop -f %MYSQLDB% || true
mysqladmin -h%MYSQLHOST% -P%MYSQLPORT% -u%MYSQLUSERNAME% -p%MYSQLPASSWORD% create %MYSQLDB%
