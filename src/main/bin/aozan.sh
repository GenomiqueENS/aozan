#!/bin/bash
BASE_DIR=`dirname $0`
SCRIPTS_DIR=$BASE_DIR/python
JYTHON=$BASE_DIR/jython.sh

cd $SCRIPTS_DIR
$JYTHON aozan.py $1
