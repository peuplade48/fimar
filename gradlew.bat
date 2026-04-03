@rem Gradle wrapper for Windows
@if "%DEBUG%"=="" @echo off
@rem Set local scope for variables
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Find java.exe
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% EQU 0 goto execute
echo ERROR: JAVA_HOME is not set correctly. >&2
goto fail
:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
%JAVA_EXE% %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
:end
if "%ERRORLEVEL%"=="0" goto mainEnd
:fail
exit /b 1
:mainEnd
endlocal
