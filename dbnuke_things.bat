@ECHO off
:: Use this script to remove all things from the RIoT database.
:: Remember to stop Tomcat running RIoT Services before running this script.
:: usage:
:: dbnuke_things.bat mongo
:: dbnuke_things.bat mysql
:: dbnuke_things.bat mssql


SET db_type=%1

SET mongo_db=riot_main

SET mysql_user=root
SET mysql_password=control123!

SET mssql_user=root
SET mssql_password=control123!


GOTO CASE_%db_type%
:CASE_mongo
    ECHO Trying Mongo:
    call mongo --version >NUL
    if %ERRORLEVEL% EQU 0 (
        echo OK
        mongo %mongo_db% --eval "db.getCollection('things').remove({})";
        mongo %mongo_db% --eval "db.getCollection('thingSnapshotIds').remove({})";
        mongo %mongo_db% --eval "db.getCollection('thingSnapshots').remove({})";
        mongo %mongo_db% --eval "db.getCollection('timeseries').remove({})";
        mongo %mongo_db% --eval "db.getCollection('timeseriesControl').remove({})";
        mongo %mongo_db% --eval "db.repairDatabase()";
    ) ELSE (
        echo mongo not detected.
    )
    GOTO END_SWITCH
:CASE_mssql
    ECHO Trying MS SQL:
    call sqlcmd -? >NUL
    if %ERRORLEVEL% EQU 0 (
        echo OK
        sqlcmd -U %mssql_user% -P %mssql_password% -Q "use riot_main; EXEC sp_msforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT all'; DELETE FROM apc_thing; DELETE FROM thingfield; DELETE FROM thingfieldvalue; DELETE FROM thingimage; DELETE FROM thingparenthistory; EXEC sp_msforeachtable 'ALTER TABLE ? CHECK CONSTRAINT all';"
    ) ELSE (
        echo sqlcmd not detected.
    )
    GOTO END_SWITCH
:CASE_mysql
    ECHO Trying MySQL:
    call mysql -V >NUL
    if %ERRORLEVEL% EQU 0 (
        echo OK
        mysql -u %mysql_user% -p%mysql_password% -e "SET FOREIGN_KEY_CHECKS = 0; TRUNCATE apc_thing; TRUNCATE thingimage; TRUNCATE thingparenthistory; SET FOREIGN_KEY_CHECKS = 1;" riot_main
    ) ELSE (
        echo mysql not detected.
    )
    GOTO END_SWITCH
:CASE_
    ECHO Please call with one parameter: mongo, mysql or mssql
:END_SWITCH
