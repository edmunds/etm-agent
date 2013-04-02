#!/bin/sh

OS=`uname`
# Determine scripts location
APP_HOME=`dirname "$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"`
parentdir=`dirname "$(cd "${abspath}/../" 2>/dev/null; echo "$PWD"/"${0##*/}")"`

GLOBAL_PROPERTIES="/deployments/edmunds/properties/common/${project.artifactId}-startup.properties"

if [ -e ${APP_HOME}/startup.properties ]; then
   . ${APP_HOME}/startup.properties
fi

LOGFILE_NAME="etm-agent.log"

if [ "${OS}" = "Darwin" ]; then
    echo "Assuming developer mode setting environment variables accordingly."
    CHECK=""
    DEFAULT_JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
    APP_HOME=`pwd`
    DEFAULT_OUTPUT_DIR="${APP_HOME}"
    DEFAULT_MIN_MEM="64m"
    DEFAULT_MAX_MEM="64m"
else
    DEFAULT_JAVA_HOME="/apps/sun-jdk-1.6"
    DEFAULT_OUTPUT_DIR="/logs/etm-agent/`hostname -s`"
    DEFAULT_MIN_MEM="64m"
    DEFAULT_MAX_MEM="128m"
    JAVA_OPTS="-server"
    COMMAND_PREFIX="nohup"
fi

if [ -e ${GLOBAL_PROPERTIES} ]; then
   . "${GLOBAL_PROPERTIES}"
fi

EXPECTED_ARGS=1
if [ $# -gt $EXPECTED_ARGS ]; then
	usage
	exit 1
fi

usage ()
{
	echo "$0 [-fg]"
	echo "-fg optional parameter to not run as a daemon."
	echo ""
}

if [ $# -eq $EXPECTED_ARGS ] && [ x"$1" = "x-fg" ]; then
	DEBUG=""
elif [ $# -eq $EXPECTED_ARGS ] && [ x"$1" != "x-fg" ]; then
	echo "Parameter $1 not recognized. See usage bellow: "
	usage
	exit 1
fi

CLASSPATH="${APP_HOME}/resources"
for value in `ls ${APP_HOME}/lib/*.jar`
do
  export CLASSPATH=${CLASSPATH}:${value}
done

if [ "x${DEBUG}" = "x" ]; then
    DEBUG=${DEFAULT_DEBUG}
fi

if [ "x${MAX_MEM}" = "x" ]; then
    MAX_MEM=${DEFAULT_MAX_MEM}
fi

if [ "x${MIN_MEM}" = "x" ]; then
   MIN_MEM=${DEFAULT_MIN_MEM}
fi

if [ "x${JAVA_HOME}" = "x" ]; then
     JAVA_HOME="${DEFAULT_JAVA_HOME}"
fi

if [ "x${OUTPUT_DIR}" = "x" ]; then
    OUTPUT_DIR=${DEFAULT_OUTPUT_DIR}
fi

if  [ -d ${OUTPUT_DIR} ]; then
   mkdir -p ${OUTPUT_DIR}
fi

JMX_AGENT_PATH=(${APP_HOME}/lib/jmxrmi-agent*.jar)

if [ $# -eq $EXPECTED_ARGS ] && [ x"$1" = "x-fg" ]; then
        ${JAVA_HOME}/bin/java -Xms${MIN_MEM} -Xmx${MAX_MEM} ${GC_OPTIONS} \
        -DOUTPUT_DIR=${OUTPUT_DIR} -DLOGFILE_NAME=${LOGFILE_NAME} \
        -javaagent:${JMX_AGENT_PATH} -Dedmunds.jmxrmi.agent.port=${JMX_PORT} \
        ${CLASS_NAME}
else
        ${COMMAND_PREFIX} ${JAVA_HOME}/bin/java -Xms${MIN_MEM} -Xmx${MAX_MEM} ${GC_OPTIONS} \
        -DOUTPUT_DIR=${OUTPUT_DIR} -DLOGFILE_NAME=${LOGFILE_NAME} \
        -javaagent:${JMX_AGENT_PATH} -Dedmunds.jmxrmi.agent.port=${JMX_PORT} \
        ${CLASS_NAME} 2>/dev/null 1>/dev/null &
fi
