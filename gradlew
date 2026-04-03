#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle wrapper script for Linux/Mac

##############################################################################
# Helper Functions
##############################################################################
die () {
    echo
    echo "ERROR: $*" >&2
    echo
    exit 1
}

# OS specific support
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in
  CYGWIN* ) cygwin=true ;;
  Darwin  ) darwin=true ;;
  MSYS* | MINGW* ) msys=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command found."
fi

APP_HOME=$( cd "${0%/*}" && pwd -P )
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
