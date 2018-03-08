@ECHO ON
:: popdb.bat - Populates the databases (MySQL + Mongo).
::
:: EXAMPLE:
:: popdb.bat DBCONN MONGOCONN POPDBNAME
:: DBCONN and MONGOCONN format is standard database net connection string
:: user/password@host:port/database
::
:: Example:
:: for mysql
:: popdb.bat root/control123!@localhost:3306/riot_main admin/control123!@localhost:27017/riot_main MojixRetail
:: for mssql
:: popdb.bat sa/control123!@localhost:1433/riot_main admin/control123!@localhost:27017/riot_main MojixRetail
:: - loads configuration details from MojixRetail, optional argument defines which dataset to run (if any)

SET SCRIPTPATH=%~dp0

IF ["%1"]==[] (
	echo "Called without any arguments - exiting."
	exit /B 1
)

:: Load configuration variables from file specified (command line first argument)
cd %SCRIPTPATH%

SET DBCONN=%1
SET MONGOCONN=%2
SET POPDBNAME=%3

call "%SCRIPTPATH%popdb_mongo.bat" %MONGOCONN%


SET PREFIXDB=DB
call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXDB% %DBCONN%


IF [%DBPORT%] == [3306] (
	echo "executing popdb-mysql.bat"
	call "%SCRIPTPATH%popdb-mysql.bat" %DBCONN% %MONGOCONN%
)
IF [%DBPORT%] == [1433] (
	echo "executing popdb-mssql.bat"
	call "%SCRIPTPATH%popdb-mssql.bat" %DBCONN% %MONGOCONN%
)

IF NOT [%POPDBNAME%]==[] (
	IF [%DBPORT%] == [3306] (
		echo "executing popdb-other-mysql.bat"
		call "%SCRIPTPATH%popdb-other-mysql.bat" %DBCONN% %MONGOCONN% %POPDBNAME%
	)
	IF [%DBPORT%] == [1433] (
		echo "executing popdb-other-mssql.bat"
		call "%SCRIPTPATH%popdb-other-mssql.bat" %DBCONN% %MONGOCONN% %POPDBNAME%
	)
)
