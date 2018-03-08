:: NOTE:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: 7-ZIP:      7-ZIP must be installer in backup server you can get it from here https://goo.gl/IFA1k0 You must unzip the folder to: C:\7zip and add it to path in System Environment Variables:
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

@echo off

SET "MSSQL_BACK_PATH=C:\Program Files\Microsoft SQL Server\MSSQL12.MSSQLSERVER\MSSQL\Backup"
SET "MSSQL_NAME=riot_main"
SET "MYSQL_USER=root"
SET "MYSQL_PASSWORD=control123!"
SET "MYSQL_DB=riot_main"
SET "BACKUP_DIR_PREFIX=backup_riotdb"
SET "SCRIPTPATH=%~dp0"
::Enable MongoDB Backup
SET "ENABLE_MONGO_BACKUP=yes"
:: Getting the timestamp:
for /F "usebackq tokens=1,2 delims==" %%i in (`wmic os get LocalDateTime /VALUE 2^>NUL`) do if '.%%i.'=='.LocalDateTime.' set timestamp=%%j
:: echo %timestamp% before
SET "timestamp=%timestamp:~0,14%%timestamp:~15,3%"

SET "BACKUP_DIR=%BACKUP_DIR_PREFIX%%timestamp%"

echo Administrative permissions required. Detecting permissions...
net session >nul 2>&1
if %errorLevel% == 0 (
	echo Success: Administrative permissions confirmed.
) else (
	echo WARNING: This script tries to create a directory and files below current path. Therefor in some cases you might want to run it with administrative permissions.
)

cd %SCRIPTPATH%
mkdir %BACKUP_DIR%
cd %BACKUP_DIR%
mkdir mysql
::backing up if the server have mssql server installed
REG QUERY HKLM\Software\Microsoft\Mssqlserver\client
if %errorLevel% == 0 (
	echo Taking MSSQL backup:
	call sqlcmd -U sa -P control123! -Q "BACKUP DATABASE [%MSSQL_NAME%] TO DISK = N'%timestamp%.bak' WITH NOFORMAT, NOINIT, NAME = N'%MSSQL_NAME%-Full Database Backup', SKIP, NOREWIND, NOUNLOAD, STATS = 10"
	copy "%MSSQL_BACK_PATH%\%timestamp%.bak" %timestamp%_mssql.bak
)

echo Taking MySQL backup:
call mysqldump -u %MYSQL_USER% -p%MYSQL_PASSWORD% --add-drop-database --add-drop-table --hex-blob --databases %MYSQL_DB% > mysql\mysqldump%timestamp%.sql 

:: Mongo Backup
if %ENABLE_MONGO_BACKUP% == yes (
echo  Taking MongDB Backup
mkdir mongodb
call mongodump --db riot_main --username admin --password control123! --authenticationDatabase admin --out mongodb\
)
cd ..
:: ------ Compress all Files
7za a -tzip "%BACKUP_DIR%.zip" "%BACKUP_DIR%"
:: ------ Deleting created folders
rd /s /q %BACKUP_DIR%
echo.
echo Backup ready in this directory: %SCRIPTPATH%
echo.
pause
:end
