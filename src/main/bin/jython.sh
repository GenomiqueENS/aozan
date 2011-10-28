#!/bin/bash

me=$(dirname $0)

AOZAN_HOME=$(dirname $0)
SGDB_LIB_HOME=/import/mimir03/lib
JYTHON_HOME=$SGDB_LIB_HOME/jython
PYTHON_MODULE_PATH=$me/python

JAVA_ARGS="-client -Xmx2048m"
JAVA_CMD=java


# Java path for Mac OS X
if [ `uname` = "Darwin" ]; then
	JAVA_CMD="JavaVM.framework/Versions/1.6/Commands/java"
fi

export JYTHONPATH=$JYTHONPATH:.
CLASSPATH="$JYTHON_HOME/jython.jar"

for j in `ls $AOZAN_HOME/lib/*.jar`
do
  CLASSPATH=$CLASSPATH:$j
done



MAIN_CLASS=fr.ens.transcriptome.aozan.Main
#CMD="$JAVA_ARGS -cp $CLASSPATH -Djython.home=$JYTHON_HOME -Dpython.module.path=$PYTHON_MODULE_PATH $MAIN_CLASS"
CMD="$JAVA_ARGS -cp $CLASSPATH -Djython.home=$JYTHON_HOME $MAIN_CLASS"

foo=0
while [ "$foo" -le $(($BASH_ARGC-1)) ]
do
  INDEX=$(($BASH_ARGC-$foo-1))
  ARG=${BASH_ARGV[$INDEX]}
  foo=$(($foo+1))
  CMD="$CMD \"$ARG\""
done


echo $CMD
echo $CMD | xargs java
