@ECHO OFF
:: popdb-parse-string.bat - Parses database url string.
::
:: EXAMPLE:
:: popdb-parse-string.bat DATABASEPREFIX USER/PASSWORD@HOST:PORT/DATABASE
:: meaning:
:: for mongo
:: popdb.bat MONGO admin/control123!@localhost:27017/riot_main
:: for mysql
:: popdb.bat MYSQL root/control123!@localhost:3306/riot_main
:: for mssql
:: popdb.bat MSSQL sa/control123!@localhost:1441/riot_main

for /F "tokens=1* delims=@" %%A in ("%2") do (
	for /F "tokens=1* delims=/" %%D in ("%%A") do (
		set %1USERNAME=%%D
		set %1PASSWORD=%%E
	)
	for /F "tokens=1* delims=/" %%F in ("%%B") do (
		for /F "tokens=1* delims=:" %%I in ("%%F") do (
			set %1HOST=%%I
			set %1PORT=%%J
		)
	    set %1DB=%%G
	)
)

@ECHO ON