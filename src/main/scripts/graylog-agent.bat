:: Graylog Agent startup script for Windows

@ECHO OFF

SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
TITLE Graylog Agent ${project.version}

if NOT DEFINED JAVA_HOME goto :jvmError

:jvmError
ECHO JAVA_HOME not set! 1>&2
EXIT /B 1

:continue
set BIN_DIR=%~dp0
FOR %%D in ("%BIN_DIR%..") DO SET AGENT_ROOT=%%~dpfD

SET AGENT_JAR="%AGENT_ROOT%\${project.artifactId}"

"%JAVA_HOME%\bin\java" ${agent.jvm-opts} -jar "%AGENT_JAR%" %*

ENDLOCAL
