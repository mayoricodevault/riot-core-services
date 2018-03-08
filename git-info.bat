::formats: json, bash, java properties
::This bat file needs these files: git.exe, head.exe, libiconv-2.dll and msys-1.0.dll to run.
::Can be downloaded from http://git-scm.com/downloads


SET OUT=%~dp0dist\extra
SET LOG=%OUT%\git-log.txt
SET STATUS=%OUT%\git-status.txt

SET JSON=%OUT%\git.json
SET PROPERTIES=%OUT%\git.properties
SET BASH=%OUT%\git.sh

mkdir %OUT%
git log | head -4 > %LOG%
git status > %STATUS%

FOR /F %%i IN ('git rev-parse --abbrev-ref HEAD') DO SET BRANCH=%%i
FOR /F %%i IN ('git rev-parse HEAD') DO SET COMMIT=%%i
git log -1 --format=%%cd>temp.txt
for /f "delims=" %%i in (temp.txt) do set DATE1=%content% %%i
git log -1 --format=%%cd --date=iso>temp2.txt
for /f "delims=" %%i in (temp2.txt) do set DATE_ISO=%content% %%i

SET YEAR=%DATE1:~20,4%
SET MONTH=%DATE_ISO:~6,2%
SET MONTH2=%DATE1:~5,3%
SET DAY=%DATE1:~9,1%
SET HOUR=%DATE1:~11,8%

SET VERSION=%YEAR%.%MONTH2%.%DAY%.%HOUR%


IF [%1] == [] (
	SET MARKETING_VERSION=2.0.6
) ELSE (
	SET MARKETING_VERSION=%1
)

:: write a json file
(
ECHO	{
ECHO	"commit" : "%COMMIT%",
ECHO	"branch" : "%BRANCH%",
ECHO	"date" : "%DATE1%",
ECHO	"marketingVersion" : "%MARKETING_VERSION%",
ECHO	"serial" : "%VERSION%"
ECHO	}
)>%JSON%

:: write a java properties file
(
ECHO	commit="%COMMIT%"
ECHO	branch="%BRANCH%"
ECHO	date="%DATE1%"
)>%PROPERTIES%

:: write a bash file
(
ECHO	SET COMMIT="%COMMIT%"
ECHO	SET BRANCH="%BRANCH%"
ECHO	SET DATE="%DATE1%"
)>%BASH%
