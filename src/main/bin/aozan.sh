#!/bin/bash
BASE_DIR=`dirname $0`
SCRIPTS_DIR=$BASE_DIR/python
JYTHON=$BASE_DIR/jython.sh

DIR=`pwd`

cd $SCRIPTS_DIR
#$JYTHON $SCRIPTS_DIR/aozan.py
$JYTHON aozan.py

cd $DIR
