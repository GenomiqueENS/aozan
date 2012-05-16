#!/bin/bash

while true; do
    read -p "Do you wish to upgrade the production version of Aozan?" yn
    case $yn in
        [Yy]* ) 

	DIR=`pwd`
	mvn clean install
	if [ $? -eq 0 ]; then
		cd ~/shares-net/lib
		rm -rf aozan-0.*
		tar xzf $DIR/target/aozan-0.*.tar.gz
	       	rm aozan
	        ln -s aozan-0.* aozan
	fi
	cd $DIR
	break;;

        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done
