@echo off

set output-file="%~dp0ct-%1-1.log"

if "%1" == "" goto HELP
if "%1" == "mkview" goto MKVIEW
if "%1" == "setcs" goto END
GOTO PRINT-OUTPUT

:MKVIEW
mkdir "%5"

:PRINT-OUTPUT
IF NOT EXIST %output-file% goto UNKNOWN-COMMAND
type %output-file%
set errorlevel = 0
GOTO END

:UNKNOWN-COMMAND
ECHO Unknown command %1, %output-file%
set errorlevel = 2
GOTO END

:HELP
echo Usage: cleartool [command] [ignored arguments]
set errorlevel = 1
GOTO END

:END
exit /b %errorlevel%