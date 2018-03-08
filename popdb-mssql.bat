@ECHO ON
:: popdb-mssql.bat - Creates MSSQL database and default tables
::
:: popdb-mssql.bat MSSQLCONN MONGOCONN
::
:: MSSQLCONN and MONGOCONN format is standard database net connection string
:: user/password@host:port/database
::
:: Example:
:: popdb-mssql.bat sa/control123!@localhost:1433/riot_main admin/control123!@localhost:27017/riot_main
:: - the arguments will be used to establish DB connections during the popdb process

SET SCRIPTPATH=%~dp0

IF [%1]==[] (
	echo "USAGE: popdb-mssql.bat MSSQLCONN MONGOCONN
    exit /b 1
)


SET PREFIXMSSQL=MSSQL
SET PREFIXMONGO=MONGO


call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMSSQL% %1
call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMONGO% %2

set MSSQLURL=jdbc:jtds:sqlserver://%MSSQLHOST%:%MSSQLPORT%/%MSSQLDB%
set MSSQLDIALECT="org.hibernate.dialect.SQLServerDialect"

echo "executing sqlcmd drop"
sqlcmd -U %MSSQLUSERNAME% -P %MSSQLPASSWORD% -S %MSSQLHOST% -Q "ALTER DATABASE %MSSQLDB% SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE %MSSQLDB%;" || echo db couldn't be dropped

echo "executing sqlcmd create"
sqlcmd -U %MSSQLUSERNAME% -P %MSSQLPASSWORD% -S %MSSQLHOST% -Q "CREATE DATABASE %MSSQLDB%; ALTER DATABASE %MSSQLDB% SET MULTI_USER"

call "%SCRIPTPATH%popdbappcore.bat" %MSSQLURL% %MSSQLUSERNAME% %MSSQLPASSWORD% %MSSQLDIALECT%

call "%SCRIPTPATH%popdbiot.bat" %MSSQLURL% %MSSQLUSERNAME% %MSSQLPASSWORD% %MSSQLDIALECT% %MONGOHOST% %MONGOPORT% %MONGODB% %MONGOUSERNAME% %MONGOPASSWORD%
