@ECHO ON
:: popdb-mysql.bat - Creates MySQL database and default tables
::
:: EXAMPLE:
:: popdb-mysql.bat MYSQLCONN MONGOCONN
::
:: MYSQLCONN and MONGOCONN format is standard database net connection string
:: user/password@host:port/database
::
:: Example:
:: popdb-mysql.bat root/control123!@localhost:3306/riot_main admin/control123!@localhost:27017/riot_main
:: - the arguments will be used to establish MYSQL connections during the popdb process

SET SCRIPTPATH=%~dp0

IF [%1]==[] (
	echo "USAGE: popdb-mysql.bat MYSQLCONN MONGOCONN"
    exit /b 1
)

SET PREFIXMYSQL=MYSQL
SET PREFIXMONGO=MONGO
SET MYSQLDIALECT="org.hibernate.dialect.MySQLDialect"


call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMYSQL% %1
call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMONGO% %2

SET MYSQLURL=jdbc:mysql://%MYSQLHOST%:%MYSQLPORT%/%MYSQLDB%

echo "executing mysqladmin drop"
mysqladmin -u %MYSQLUSERNAME% -p%MYSQLPASSWORD% drop -f %MYSQLDB% || echo db couldn't be dropped

echo "executing mysqladmin create"
mysqladmin -u %MYSQLUSERNAME% -p%MYSQLPASSWORD% create %MYSQLDB%

call "%SCRIPTPATH%popdbappcore.bat" %MYSQLURL% %MYSQLUSERNAME% %MYSQLPASSWORD% %MYSQLDIALECT%

call "%SCRIPTPATH%popdbiot.bat" %MYSQLURL% %MYSQLUSERNAME% %MYSQLPASSWORD% %MYSQLDIALECT% %MONGOHOST% %MONGOPORT% %MONGODB% %MONGOUSERNAME% %MONGOPASSWORD%
