#!/bin/bash
DIR=`pwd`
mvn clean install
if [ $? -eq 0 ]; then
	cd ~/home-net
	rm -rf aozan-0.*
	tar xzf $DIR/target/aozan-0.*.tar.gz
	rm aozan
	ln -s aozan-0.* aozan
fi
cd $DIR
