@echo off
REM ======================================
REM  copy_chp_windows.bat
REM  1) Ensures each server folder has
REM     run_mcserver.bat from GitHub
REM  2) Copies plugin jars
REM  3) Injects or overwrites environment
REM     variables in run_mcserver.bat
REM  4) Starts only the main server
REM ======================================

::::::::::::::::::::::::::::::::::::::::::::::::::::::
::               CONFIGURATION                     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: Server folders
set "CHP_SERVER=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server"
set "CHP_SERVER_188=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_188"
set "CHP_SERVER_194=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_194"
set "CHP_SERVER_1102=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_1102"
set "CHP_SERVER_1112=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_1112"
set "CHP_SERVER_1122=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_1122"
set "CHP_SERVER_1132=C:\Users\domin\Desktop\Work\Minecraft - Java Development\MC Servers\personal_chp_test_server_1132"

:: Plugin jar path
set "CHP_PAPER_JAR=C:\Users\domin\Desktop\Work\Minecraft - Java Development\WorldwideChat\WorldwideChat\paper-target\WorldwideChat-paper.jar"

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

REM 2) If run_mcserver.bat not found, clone fresh script from GitHub
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

REM 4) Inject/overwrite environment variables inside run_mcserver.bat
call :inject_env_vars "%SERVER_DIR%\run_mcserver.bat" "%PROJECT_NAME%" "%SERVER_DIR%"

goto :eof


::::::::::::::::::::::::::::::::::::::::::::::::::::::
::   SUBROUTINE: inject_env_vars
::   Removes any old set PROJECT_NAME / SERVER_DIR,
::   then inserts them near the top (after @echo off).
::
::   %1 = path to run_mcserver.bat
::   %2 = PROJECT_NAME
::   %3 = SERVER_DIR
::::::::::::::::::::::::::::::::::::::::::::::::::::::
:inject_env_vars
set "SCRIPT_FILE=%~1"
set "NEW_PROJECT=%~2"
set "NEW_SERVERDIR=%~3"

if not exist "%SCRIPT_FILE%" (
    echo [inject_env_vars] WARNING: %SCRIPT_FILE% not found.
    goto :eof
)

REM Create a temporary file
set "TMPFILE=%TEMP%\runmcserver_%RANDOM%.tmp"
if exist "%TMPFILE%" del "%TMPFILE%"

set "FOUND_ECHO_OFF="
set "INSERTED_VARS="

(for /f "usebackq delims=" %%L in ("%SCRIPT_FILE%") do (
    REM Check if line contains "set PROJECT_NAME=" or "set SERVER_DIR="
    echo(%%L| findstr /i /b "set PROJECT_NAME= set SERVER_DIR=" >nul
    if not errorlevel 1 (
        REM It's an old line setting PROJECT_NAME or SERVER_DIR; skip it
        REM (We remove these lines so we can re-inject fresh ones)
        rem echo [DEBUG] removing old line: %%L
        rem skip
    ) else (
        REM Keep the line as-is, but watch for "@echo off"
        if defined FOUND_ECHO_OFF (
            REM Already found @echo off, keep lines
            echo(%%L
        ) else (
            REM If not found @echo off yet, check if this line is @echo off
            echo(%%L | findstr /i "^@echo off$" >nul
            if not errorlevel 1 (
                REM This line is @echo off
                echo(%%L
                set "FOUND_ECHO_OFF=1"
                REM Now insert our new lines right below it (only once)
                if not defined INSERTED_VARS (
                    echo rem --- Overwritten environment variables ---
                    echo set PROJECT_NAME=%NEW_PROJECT%
                    echo set SERVER_DIR=%NEW_SERVERDIR%
                    set "INSERTED_VARS=1"
                )
            ) else (
                REM Just a normal line
                echo(%%L
            )
        )
    )
))>"%TMPFILE%"

REM If we never found @echo off, we insert the lines at the top:
if not defined FOUND_ECHO_OFF (
    echo [inject_env_vars] No "@echo off" found, injecting at top of file.
    >"%SCRIPT_FILE%" (
        echo @echo off
        echo rem --- Overwritten environment variables ---
        echo set PROJECT_NAME=%NEW_PROJECT%
        echo set SERVER_DIR=%NEW_SERVERDIR%
    )
    type "%TMPFILE%" >> "%SCRIPT_FILE%"
) else (
    REM We already inserted the lines after @echo off
    copy /Y "%TMPFILE%" "%SCRIPT_FILE%" >nul
)

del "%TMPFILE%"
goto :eof


::::::::::::::::::::::::::::::::::::::::::::::::::::::
::                   MAIN FLOW                     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::

echo --- Setting up MC servers ---

:: Example: you want them all "paper" with different versions
call :setup_mc_script "%CHP_SERVER%"       "paper" "1.21.4"
call :setup_mc_script "%CHP_SERVER_188%"   "paper" "1.8.8"
call :setup_mc_script "%CHP_SERVER_194%"   "paper" "1.9.4"
call :setup_mc_script "%CHP_SERVER_1102%"  "paper" "1.10.2"
call :setup_mc_script "%CHP_SERVER_1112%"  "paper" "1.11.2"
call :setup_mc_script "%CHP_SERVER_1122%"  "paper" "1.12.2"
call :setup_mc_script "%CHP_SERVER_1132%"  "paper" "1.13.2"

echo --- Copying plugin jars ---

copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_188%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_194%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_1102%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_1112%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_1122%\plugins"
copy /Y "%CHP_PAPER_JAR%" "%CHP_SERVER_1132%\plugins"

echo --- Finally, launching the MAIN server only ---
echo Starting server at: %CHP_SERVER%
call "%CHP_SERVER%\run_mcserver.bat"

echo Done!
exit /b 0