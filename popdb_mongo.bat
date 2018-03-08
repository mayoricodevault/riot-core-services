:: popdb_mongo.bat - Nukes mongo database.
::
:: EXAMPLE:
:: popdb_mongo.bat MONGOCONN
:: meaning:
:: popdb_mongo.bat localhost 27017 riot_main
:: - loads configuration details from MojixRetail, optional argument defines which dataset to run (if any)

SET SCRIPTPATH=%~dp0

echo Drop Mongo database...

SET PREFIXMONGO=MONGO

call "%SCRIPTPATH%popdb-parse-connstring.bat" %PREFIXMONGO% %1

mongo %MONGOHOST%:%MONGOPORT%/%MONGODB% -u %MONGOUSERNAME% -p %MONGOPASSWORD% --authenticationDatabase admin --eval "db.getCollectionNames().forEach(function(c) { if (c.indexOf(\"system.\") == -1) db[c].drop(); })"
mongo %MONGOHOST%:%MONGOPORT%/%MONGODB% -u %MONGOUSERNAME% -p %MONGOPASSWORD% --authenticationDatabase admin --eval "db.system.js.remove({_id : /^vizixFunction/}); db.system.js.remove({_id: \"JSONFormatter\"});"

