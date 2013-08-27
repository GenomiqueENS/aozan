#!/bin/bash

while true; do
    read -p "Do you wish to upgrade the production version of Aozan?" yn
    case $yn in
        [Yy]* ) 

	DIR=`pwd`
	mvn clean install
	if [ $? -eq 0 ]; then
		cd ~/home-net/
		rm -rf aozan-1.*
		tar xzf $DIR/target/aozan-1.*.tar.gz
	       	rm aozan
	        ln -s aozan-1.* aozan
	fi
	cd $DIR
	break;;

        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done
