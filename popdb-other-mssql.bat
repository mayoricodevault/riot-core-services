@ECHO ON
:: Script to run popdb for other database.
::
:: EXAMPLE:
:: popdb-other-mssql.bat MSSQLCONN MONGOCONN POPDBNAME
::
:: MSSQLCONN and MONGOCONN format is standard database net connection string
:: user/password@host:port/database
::
:: Example:
:: popdb-other-mssql.bat sa/control123!@localhost:1433/riot_main admin/control123!@localhost:27017/riot_main MojixGM


SET SCRIPTPATH=%~dp0
SET CLASS=com.tierconnect.riot.iot.popdb.PopDB
SET FATJAR=riot-core-services-all-*.jar
SET JAVAPROPERTIES=-Dlog4j.configuration=log4j.stdout.properties

:: findJar sets the correct %CP%
CALL "%SCRIPTPATH%findJar.bat" %FATJAR%


SET PREFIXMYSQL=MYSQL
SET PREFIXMONGO=MONGO
SET MYSQLDIALECT="org.hibernate.dialect.MySQLDialect"

call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMYSQL% %1
call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMONGO% %2

SET MSSQLURL=jdbc:jtds:sqlserver://%MSSQLHOST%:%MSSQLPORT%/%MSSQLDB%
SET MSSQLDIALECT="org.hibernate.dialect.SQLServerDialect"

SET POPDBNAME=%3

IF NOT [%1]==[] (
SET JAVAPROPERTIES=%JAVAPROPERTIES% -Dhibernate.connection.url=%MSSQLURL% -Dhibernate.connection.username=%MSSQLUSERNAME% -Dhibernate.connection.password=%MSSQLPASSWORD%  -Dhibernate.dialect=%MSSQLDIALECT% -Dmongo.host=%MONGOHOST% -Dmongo.port=%MONGOPORT% -Dmongo.db=%MONGODB% -Dmongo.username=%MONGOUSERNAME% -Dmongo.password=%MONGOPASSWORD% -D2f53086555b7fbb940ce78616ff212e5=false
)

java %JAVAPROPERTIES% -cp %CP% %CLASS%%POPDBNAME% %*
