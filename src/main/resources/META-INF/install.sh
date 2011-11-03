#!/bin/bash

#####################################
# Install script for ETM Agent      #
#####################################

abspath=`dirname "$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"`
parentdir=`dirname "$(cd "${abspath}/../" 2>/dev/null; echo "$PWD"/"${0##*/}")"`
propertiesDir="/deployments/edmunds/properties/common/"
keyLocation="${propertiesDir}/etm-agent_key"
hostsLocation="${propertiesDir}/apache-slave-hosts.conf"
whoami=`whoami`

# Verify properties directory exists
if [ ! -d "/deployments/edmunds/properties/common/" ];
then
    mkdir -p /deployments/edmunds/properties/common/
    chown webapps:webapps /deployments/edmunds/properties/common/
elif [ -d "/deployments/edmunds/properties/common/" ];
then
    chown webapps:webapps /deployments/edmunds/properties/common/
fi

ln -sfn  ${parentdir} /deployments/${project.artifactId}/${project.artifactId}

chmod +x ${parentdir}/*.sh
chmod +x ${parentdir}/init/*

if [ "x${whoami}" = "xwebapps" ] ; then
	if [ -e ${keyLocation} ] && [ -e ${hostsLocation} ]; then
		/bin/sh /deployments/${project.artifactId}/${project.artifactId}/init/${project.artifactId} stop
		/bin/sh /deployments/${project.artifactId}/${project.artifactId}/init/${project.artifactId} start
		if [ $? -eq 0 ]; then
			for apacheHost in `cat ${hostsLocation}`; do
                        	echo "restarting $apacheHost"
                        	ssh -o strictHostKeyChecking=no -i ${keyLocation} $apacheHost restart
                        	if [ $? -eq 1 ];
                        	then
                                	echo "Restart of $apacheHost failed..."
                                	exit 1
                        	elif [ $? -eq 0 ];
                        	then
                                	sleep 30
                        	fi
                	done
                	exit $?
		fi
	else
		/bin/sh /deployments/${project.artifactId}/${project.artifactId}/init/${project.artifactId} stop
                /bin/sh /deployments/${project.artifactId}/${project.artifactId}/init/${project.artifactId} start
		exit $?
	fi
else
	echo "ERROR: This script should only be run by the webapps user"
	exit 127
fi
