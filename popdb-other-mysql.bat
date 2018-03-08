@ECHO ON
:: Script to run popdb for other database.
::::
  :: EXAMPLE:
  :: popdb-other-mssql.bat MYSQLCONN MONGOCONN POPDBNAME
  ::
  :: MYSQLCONN and MONGOCONN format is standard database net connection string
  :: user/password@host:port/database
  ::
  :: Example:
  :: popdb-other-mssql.bat root/control123!@localhost:1433/riot_main admin/control123!@localhost:27017/riot_main MojixRetail

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

SET MYSQLURL=jdbc:mysql://%MYSQLHOST%:%MYSQLPORT%/%MYSQLDB%
SET MYSQLDIALECT="org.hibernate.dialect.MySQLDialect"

SET POPDBNAME=%3

IF NOT [%1]==[] (
SET JAVAPROPERTIES=%JAVAPROPERTIES% -Dhibernate.connection.url=%MYSQLURL% -Dhibernate.connection.username=%MYSQLUSERNAME% -Dhibernate.connection.password=%MYSQLPASSWORD% -Dhibernate.dialect=%MYSQLDIALECT% -Dmongo.host=%MONGOHOST% -Dmongo.port=%MONGOPORT% -Dmongo.db=%MONGODB% -Dmongo.username=%MONGOUSERNAME% -Dmongo.password=%MONGOPASSWORD% -D2f53086555b7fbb940ce78616ff212e5=false
)

java %JAVAPROPERTIES% -cp %CP% %CLASS%%POPDBNAME% %*
