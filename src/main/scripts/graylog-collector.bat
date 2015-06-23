:: Graylog Collector startup script for Windows

@ECHO OFF

SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
TITLE Graylog Collector ${project.version}

IF DEFINED JAVA_HOME goto :continue

:: JAVA_HOME needs to be set to find the JVM.
:jvmError
ECHO JAVA_HOME not set! 1>&2
EXIT /B 1

:: Execute the JAR.
:continue
set COLLECTOR_BIN_DIR=%~dp0

:: Get root directory of the Collector.
FOR %%D in ("%COLLECTOR_BIN_DIR%..") DO SET COLLECTOR_ROOT=%%~dpfD

SET COLLECTOR_JAR=%COLLECTOR_ROOT%\${project.artifactId}.jar
SET COLLECTOR_JVM_OPTIONS=-Djava.library.path="%COLLECTOR_ROOT%\lib\sigar" -Dfile.encoding=UTF-8 ${collector.jvm-opts}

"%JAVA_HOME%\bin\java" %COLLECTOR_JVM_OPTIONS% -jar "%COLLECTOR_JAR%" %*

ENDLOCAL
