#!/usr/bin/env bash

#
# This script set the right classpath and start the application
#
# Author : Laurent Jourdren
#

# Function to create lib paths
make_paths() {

    local RESULT=
    for lib in `ls $1`
    do
        if [ -f $1/$lib ]; then
            RESULT=$RESULT:$1/$lib
        fi
    done

    echo $RESULT
}

# Read link to get aozan.sh path
REAL_PATH=$(readlink -f $0)

# Get the path to this script
BASEDIR=$(dirname $REAL_SCRIPT_PATH)

# Set the libraries path
LIBDIR=$BASEDIR/lib

# Set the memory in MiB needed by the application (only Java part, not external tools)
# By Default 4096
if [ -n "$AOZAN_MEMORY" ]; then
	MEMORY=$AOZAN_MEMORY
else
	MEMORY=4096
fi

# Additional JVM options
if [ -n "$AOZAN_JVM_OPTS" ]; then
	JVM_OPTS=$AOZAN_JVM_OPTS
else
	JVM_OPTS="-server"
fi

# Add here your plugins and dependencies
if [ -n "$AOZAN_PLUGINS" ]; then
	PLUGINS=$AOZAN_PLUGINS
else
	PLUGINS=
fi

# Parse options
OPTERR=0
while getopts "j:m:J:p:" OPTION
do
	case $OPTION in
		j)
			JAVA_HOME=$OPTARG
		;;
		m)
			MEMORY=$OPTARG
		;;
		J)
			JVM_OPTS=$OPTARG
		;;
		p)
			PLUGINS=$OPTARG
		;;
		w)
			cd $OPTARG
		;;
	esac
done

# Set the path to java
if [ -n "$AOZAN_JAVA_HOME" ] ; then
	JAVA_CMD="$AOZAN_JAVA_HOME/bin/java"
elif [ -n "$JAVA_HOME" ] ; then
	JAVA_CMD="$JAVA_HOME/bin/java"
else
	JAVA_CMD="java"
fi

# Set the temporary directory
TMPDIR_JVM_OPT=""
if [ -n "$TMPDIR" ]; then
	TMPDIR_JVM_OPT="-Djava.io.tmpdir=$TMPDIR"
fi

COMMON_LIBS=$(make_paths $LIBDIR)
LOCAL_LIBS=$(make_paths $LIBDIR/local)
PLUGINS_LIBS=$(make_paths $AOZAN_PLUGINS)
APP_CLASSPATH=$COMMON_LIBS:$LOCAL_LIBS:$PLUGINS:$PLUGINS_LIBS

# Force language
#export LANG=C

# Launch Aozan
$JAVA_CMD \
		$TMPDIR_JVM_OPT \
		$JVM_OPTS \
		-Xmx${MEMORY}m \
		-cp $APP_CLASSPATH \
		-Dapplication.script.path="$0" \
		-Dapplication.classpath=$APP_CLASSPATH \
		-Dapplication.memory=$MEMORY \
		-Dapplication.launch.mode=local \
		-Dapplication.launch.script.path="$0" \
		-Dapplication.path="$BASEDIR" \
		fr.ens.biologie.genomique.aozan.aozan3.Main "$@"

