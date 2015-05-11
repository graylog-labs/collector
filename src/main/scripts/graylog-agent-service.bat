:: Graylog Agent service management for Windows.

@ECHO OFF

SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
TITLE Graylog Agent Service ${project.version}

:: Check if JAVA_HOME environment variable is set.
IF DEFINED JAVA_HOME goto :dispatchAction

:: Exit if there is no JAVA_HOME set.
ECHO JAVA_HOME not set! 1>&2
EXIT /B 1

:dispatchAction
:: Get the directory of this script. NEEDS to run before argument parsing!
SET AGENT_BIN_DIR=%~dp0

:: Get the ACTION and SERVICE_NAME from the script arguments.
IF "x%1x" == "xx" GOTO usage
SET ACTION=%1
SHIFT
IF "x%1x" == "xx" GOTO usage
SET SERVICE_NAME=%1

:: Get root directory of the Agent.
FOR %%D in ("%AGENT_BIN_DIR%..") DO SET AGENT_ROOT=%%~dpfD

:: Detect if we are running on 32bit or 64bit Windows.
"%JAVA_HOME%\bin\java" -version 2>&1 | "%windir%\System32\find" "64-Bit" >nul:
IF errorlevel 1 (SET ARCH=x86) ELSE (SET ARCH=x64)

:: Use the correct executable based on the architecture.
SET PROCRUN="%AGENT_BIN_DIR%\windows\graylog-agent-service-%ARCH%.exe"

:: Dispatch the supported commands, show usage otherwise.
IF /i %ACTION% == install GOTO actionInstallCheck
IF /i %ACTION% == uninstall GOTO actionUninstall
IF /i %ACTION% == manage GOTO actionManage
IF /i %ACTION% == start GOTO actionStart
IF /i %ACTION% == stop GOTO actionStop

:usage
ECHO.
ECHO Usage: %~nx0 install^|uninstall^|manage^|start^|stop SERVICE_NAME
ECHO.
ECHO Example:
ECHO.
ECHO * Install Agent as service with service name "GA"
ECHO     graylog-agent-service.bat install GA
GOTO:EOF

:actionInstallCheck
ECHO Installing service for Graylog Agent %AGENT_VERSION%
ECHO.
ECHO Service name: "%SERVICE_NAME%"
ECHO JAVA_HOME:    "%JAVA_HOME%"
ECHO ARCH:         "%ARCH%"
ECHO.

:: Select the correct JVM DLL.
SET JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
IF EXIST "%JVM_DLL%" GOTO actionInstall
SET JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
IF EXIST "%JVM_DLL%" GOTO actionInstall
SET JVM_DLL=%JAVA_HOME%\bin\client\jvm.dll

IF EXIST "%JVM_DLL%" (
ECHO WARNING: JAVA_HOME points to a JRE and not JDK installation; a client (not a server^) JVM will be used...
) ELSE (
ECHO ERROR: Invalid Java installation (no jvm.dll found in "%JAVA_HOME%"^). Abort...
GOTO:EOF
)

:actionInstall
SET AGENT_VERSION=${project.version}
SET AGENT_JAR="%AGENT_ROOT%\${project.artifactId}.jar"
SET AGENT_CLASS=org.graylog.collector.cli.Main
SET AGENT_JVM_MS=12m
SET AGENT_JVM_MX=64m
SET AGENT_JVM_OPTIONS=-Djava.library.path=%AGENT_ROOT%\lib\sigar -Dfile.encoding=UTF-8 ${agent.jvm-opts}
SET AGENT_STOP_TIMEOUT=0
SET AGENT_STARTUP=auto
SET AGENT_MODE=jvm
SET AGENT_START_METHOD=main
SET AGENT_START_PARAMS="server;-f;%AGENT_ROOT%\config\agent.conf"
SET AGENT_STOP_METHOD=stop
SET AGENT_PID_FILE="%SERVICE_NAME%.pid"
SET AGENT_LOG_DIR="%AGENT_ROOT%\logs"
SET AGENT_LOG_OPTIONS=--LogPath "%AGENT_LOG_DIR%" --LogPrefix "graylog-agent" --StdError auto --StdOutput auto

"%PROCRUN%" //IS//%SERVICE_NAME% --Classpath "%AGENT_JAR%" --Jvm "%JVM_DLL%" --JvmMs %AGENT_JVM_MS% --JvmMx %AGENT_JVM_MX% --JvmOptions %AGENT_JVM_OPTIONS: =#% --StartPath "%AGENT_ROOT%" --Startup %AGENT_STARTUP% --StartMode %AGENT_MODE% --StartClass %AGENT_CLASS% --StartMethod %AGENT_START_METHOD% --StartParams %AGENT_START_PARAMS% --StopMode %AGENT_MODE% --StopClass %AGENT_CLASS% --StopMethod %AGENT_STOP_METHOD% --StopTimeout %AGENT_STOP_TIMEOUT% --PidFile "%AGENT_PID_FILE%" --DisplayName "Graylog Agent (%SERVICE_NAME%)" --Description "Graylog Agent %AGENT_VERSION% service. See http://www.graylog.org/ for details." %AGENT_LOG_OPTIONS%

IF NOT errorlevel 1 GOTO actionInstallSuccess
ECHO ERROR: Failed to install service: %SERVICE_NAME%
GOTO:EOF

:actionInstallSuccess
ECHO Service '%SERVICE_NAME%' has been installed
GOTO:EOF

:actionUninstall
:: Remove the service
"%PROCRUN%" //DS//%SERVICE_NAME% %AGENT_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionUninstallSuccess
ECHO ERROR: Failed to uninstall service: %SERVICE_NAME%
GOTO:EOF

:actionUninstallSuccess
ECHO Service '%SERVICE_NAME%' has been removed
GOTO:EOF

:actionManage
SET PRUNMGR=%AGENT_BIN_DIR%\windows\graylog-agent-service-manager.exe
"%PRUNMGR%" //ES//%SERVICE_NAME%
IF NOT errorlevel 1 GOTO actionManageSuccess
ECHO ERROR: Failed to start service manager for service: %SERVICE_NAME%
GOTO:EOF

:actionManageSuccess
ECHO Successfully ran service manager for service: %SERVICE_NAME%
GOTO:EOF

:actionStart
"%PROCRUN%" //ES//%SERVICE_NAME% %AGENT_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionStartSuccess
ECHO ERROR: Failed to start service: %SERVICE_NAME%
GOTO:EOF

:actionStartSuccess
ECHO Service '%SERVICE_NAME%' has been started
GOTO:EOF

:actionStop
"%PROCRUN%" //SS//%SERVICE_NAME% %AGENT_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionStopSuccess
ECHO ERROR: Failed to stop service: %SERVICE_NAME%
GOTO:EOF

:actionStopSuccess
ECHO Service '%SERVICE_NAME%' has been stopped
GOTO:EOF
