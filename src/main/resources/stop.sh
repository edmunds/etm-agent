#!/bin/bash

# Go get the PID and set a variable
APP="${project.artifactId}"
PID_APP=`ps ax|grep java|grep $APP|grep -v "grep"|awk '{print $1}'`
# end of variable set

# Try to kill nicely first, wait 5 seconds then kill -9 if its still running
if [ "x${PID_APP}" != "x" ]; then
	kill $PID_APP
	sleep 5
	if [ -d /proc/$PID_APP ]; then
	        kill -9 $PID_APP
       		exit 1
	else   
       		echo "$APP: SHUTDOWN"
        	exit 0
	fi
else
	echo "$APP: NOT RUNNING"	
	exit 1
fi
