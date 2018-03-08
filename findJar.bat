@ECHO ON
:: Find jar path
:: Usage:  findFatJar.bat <jar_name>

SET JAR=%1

SET SCRIPTPATH=%~dp0

SET JAVAPROPERTIES=-Dlog4j.configuration=log4j.stdout.properties

IF EXIST "%SCRIPTPATH%%JAR%" (
	SET CP="%SCRIPTPATH%%JAR%"
) ELSE IF EXIST "%SCRIPTPATH%build\libs\%JAR%" (
	SET CP="%SCRIPTPATH%build\libs\%JAR%"
) ELSE (
	ECHO "Jar file not found - exiting."
	EXIT /B 1
)

:: use this trick to replace the asterisk with specific name
FOR %%f IN (%CP%) DO (
  SET CP="%%f"
)

ECHO "Using this jar file: %CP%"