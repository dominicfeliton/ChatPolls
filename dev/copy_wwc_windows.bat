@echo off
REM ======================================
REM  copy_wwc_windows.bat
REM  1) Ensures each server folder has
REM     run_mcserver.bat from GitHub
REM  2) Copies plugin jars
REM  3) Starts only the main server
REM ======================================

::::::::::::::::::::::::::::::::::::::::::::::::::::::
::               CONFIGURATION                     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: Server folders
set "WWC_SERVER=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server"
set "WWC_SERVER_188=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_188"
set "WWC_SERVER_194=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_194"
set "WWC_SERVER_1102=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_1102"
set "WWC_SERVER_1112=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_1112"
set "WWC_SERVER_1122=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_1122"
set "WWC_SERVER_1132=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_wwc_test_server_1132"

:: Plugin jar path
set "WWC_PAPER_JAR=C:\Users\domin\Desktop\Work\Minecraft - Java Development\WorldwideChat\WorldwideChat\paper-target\WorldwideChat-paper.jar"

::::::::::::::::::::::::::::::::::::::::::::::::::::::
::   SUBROUTINE: setup_mc_script
::
::   %1 = server folder path
::   %2 = project name (paper, spigot, folia, velocity)
::   %3 = MC version (e.g. 1.20.1)
::::::::::::::::::::::::::::::::::::::::::::::::::::::
:setup_mc_script
set "SERVER_DIR=%~1"
set "PROJECT_NAME=%~2"
set "MC_VERSION=%~3"

REM 1) Create folder if doesn't exist
if not exist "%SERVER_DIR%" (
    echo [setup_mc_script] Creating folder: %SERVER_DIR%
    mkdir "%SERVER_DIR%"
)

REM 2) If run_mcserver.bat not found, do a temp clone
if not exist "%SERVER_DIR%\run_mcserver.bat" (
    echo [setup_mc_script] run_mcserver.bat NOT found => cloning fresh script...
    set "TMPDIR=%TEMP%\tempclone_%RANDOM%"
    mkdir "%TMPDIR%"
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "%TMPDIR%"
    if errorlevel 1 (
        echo [setup_mc_script] ERROR: Git clone failed. Aborting.
        rd /S /Q "%TMPDIR%"
        goto :eof
    )
    xcopy "%TMPDIR%\*" "%SERVER_DIR%\" /E /Y >nul
    rd /S /Q "%TMPDIR%"
    echo [setup_mc_script] Copied fresh script files into %SERVER_DIR%.
)

REM 3) Write MC version to current_version.txt
if not "%MC_VERSION%"=="" (
    echo %MC_VERSION% > "%SERVER_DIR%\current_version.txt"
)

REM 4) Inject "set PROJECT_NAME=..." at end of run_mcserver.bat
if exist "%SERVER_DIR%\run_mcserver.bat" (
    (
      echo rem Set PROJECT_NAME for Windows
      echo set PROJECT_NAME=%PROJECT_NAME%
    ) >> "%SERVER_DIR%\run_mcserver.bat"
) else (
    echo [setup_mc_script] WARNING: run_mcserver.bat still missing in %SERVER_DIR%.
)

goto :eof

::::::::::::::::::::::::::::::::::::::::::::::::::::::
::                   MAIN FLOW                     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::

echo --- Setting up MC servers ---

:: Example: you want them all to be "paper" with different MC versions
call :setup_mc_script "%WWC_SERVER%"       "paper" "1.21.4"
call :setup_mc_script "%WWC_SERVER_188%"   "paper" "1.8.8"
call :setup_mc_script "%WWC_SERVER_194%"   "paper" "1.9.4"
call :setup_mc_script "%WWC_SERVER_1102%"  "paper" "1.10.2"
call :setup_mc_script "%WWC_SERVER_1112%"  "paper" "1.11.2"
call :setup_mc_script "%WWC_SERVER_1122%"  "paper" "1.12.2"
call :setup_mc_script "%WWC_SERVER_1132%"  "paper" "1.13.2"

echo --- Copying plugin jars ---

copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_188%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_194%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_1102%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_1112%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_1122%\plugins"
copy /Y "%WWC_PAPER_JAR%" "%WWC_SERVER_1132%\plugins"

echo --- Finally, launching the MAIN server only ---
echo Starting server at: %WWC_SERVER%
call "%WWC_SERVER%\run_mcserver.bat"

echo Done!
exit /b 0