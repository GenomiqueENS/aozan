'''
Aozan autorun file.
Created on 22 aug. 2012

@author: Laurent Jourdren
'''
import sys
import aozan

if len(sys.argv) < 2:
    print "No configuration file define in command line.\nSyntax: aozan.sh conf_file"
    sys.exit(1)
else:
    aozan.aozan_main(sys.argv[1])


