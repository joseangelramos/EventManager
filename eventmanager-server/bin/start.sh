#!/bin/sh

#===========================================================================================
# Java Environment Setting
#===========================================================================================
set -e
#La configuracion del servidor puede ser inconsistente, agregar estas configuraciones para evitar problemas
export LANG=en_US.UTF-8
export LC_CTYPE=en_US.UTF-8
export LC_ALL=en_US.UTF-8

TMP_JAVA_HOME="/opt/java/jdk8"

#detect operating system.
OS=$(uname -o)

function is_java8 {
        local _java="$1"
        [[ -x "$_java" ]] || return 1
        [[ "$("$_java" -version 2>&1)" =~ 'java version "1.8' || "$("$_java" -version 2>&1)" =~ 'openjdk version "1.8' ]] || return 2
        return 0
}

#0(not running),  1(is running)
#function is_proxyRunning {
#        local _pid="$1"
#        local pid=`ps ax | grep -i 'com.gcote.eventmanager.server.boot.ProxyStartup' |grep java | grep -v grep | awk '{print $1}'|grep $_pid`
#        if [ -z "$pid" ] ; then
#            return 0
#        else
#            return 1
#        fi
#}

function get_pid {
	local ppid=""
	if [ -f ${PROXY_HOME}/bin/pid.file ]; then
		ppid=$(cat ${PROXY_HOME}/bin/pid.file)
	else
		if [[ $OS =~ Msys ]]; then
			# Pedo en Msys que es posible que no pueda eliminar el proceso
			ppid=`jps -v | grep -i "com.gcote.eventmanager.server.boot.ProxyStartup" | grep java | grep -v grep | awk -F ' ' {'print $1'}`
		elif [[ $OS =~ Darwin ]]; then
			# Problemas conocidos: grep java puede no ser capaz de identificar con precision el proceso java
			ppid=$(/bin/ps -o user,pid,command | grep "java" | grep -i "com.gcote.eventmanager.server.boot.ProxyStartup" | grep -Ev "^root" |awk -F ' ' {'print $2'})
		else
			#Es necesario identificar el proceso con la mayor precision posible en el servidor Linux.
			ppid=$(ps -C java -o user,pid,command --cols 99999 | grep -w $PROXY_HOME | grep -i "com.gcote.eventmanager.server.boot.ProxyStartup" | grep -Ev "^root" |awk -F ' ' {'print $2'})
		fi
	fi
	echo "$ppid";
}


if [[ -d "$TMP_JAVA_HOME" ]] && is_java8 "$TMP_JAVA_HOME/bin/java"; then
        JAVA="$TMP_JAVA_HOME/bin/java"
elif [[ -d "$JAVA_HOME" ]] && is_java8 "$JAVA_HOME/bin/java"; then
        JAVA="$JAVA_HOME/bin/java"
elif  is_java8 "/opt/java/jdk8/bin/java"; then
    JAVA="/opt/java/jdk8/bin/java";
elif  is_java8 "/opt/java/jdk1.8/bin/java"; then
    JAVA="/opt/java/jdk1.8/bin/java";
elif  is_java8 "/opt/java/jdk/bin/java"; then
    JAVA="/opt/java/jdk/bin/java";
elif is_java8 "$(which java)"; then
        JAVA="$(which java)"
else
        echo -e "ERROR\t java(1.8) no encontrada, abortando operacion."
        exit 9;
fi

echo "proxy utiliza la java localizada en= "$JAVA

PROXY_HOME=`cd "./.." && pwd`

export PROXY_HOME

export PROXY_LOG_HOME=${PROXY_HOME}/logs

echo "PROXY_HOME : ${PROXY_HOME}, PROXY_LOG_HOME : ${PROXY_LOG_HOME}"

function make_logs_dir {
        if [ ! -e "${PROXY_LOG_HOME}" ]; then mkdir -p "${PROXY_LOG_HOME}"; fi
}

error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}

export JAVA_HOME

#===========================================================================================
# JVM Configuration
#===========================================================================================
#if [ $1 = "prd" -o $1 = "benchmark" ]; then JAVA_OPT="${JAVA_OPT} -server -Xms2048M -Xmx4096M -Xmn2048m -XX:SurvivorRatio=4"
#elif [ $1 = "sit" ]; then JAVA_OPT="${JAVA_OPT} -server -Xms256M -Xmx512M -Xmn256m -XX:SurvivorRatio=4"
#elif [ $1 = "dev" ]; then JAVA_OPT="${JAVA_OPT} -server -Xms128M -Xmx256M -Xmn128m -XX:SurvivorRatio=4"
#fi

#JAVA_OPT="${JAVA_OPT} -server -Xms2048M -Xmx4096M -Xmn2048m -XX:SurvivorRatio=4"
JAVA_OPT=`cat ${PROXY_HOME}/conf/server.env | grep APP_START_JVM_OPTION::: | awk -F ':::' {'print $2'}`
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:SurvivorRatio=8 -XX:MaxGCPauseMillis=50"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${PROXY_HOME}/logs/proxy_gc_%p.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintAdaptiveSizePolicy"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${PROXY_HOME}/logs -XX:ErrorFile=${PROXY_HOME}/logs/hs_err_%p.log"
JAVA_OPT="${JAVA_OPT} -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:+AlwaysPreTouch"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=8G"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages -XX:-UseBiasedLocking"
JAVA_OPT="${JAVA_OPT} -Dio.netty.leakDetectionLevel=advanced"
JAVA_OPT="${JAVA_OPT} -Dio.netty.allocator.type=pooled"
JAVA_OPT="${JAVA_OPT} -Djava.security.egd=file:/dev/./urandom"
JAVA_OPT="${JAVA_OPT} -Dlog4j.configurationFile=${PROXY_HOME}/conf/log4j2.xml"
JAVA_OPT="${JAVA_OPT} -Dproxy.log.home=${PROXY_LOG_HOME}"
JAVA_OPT="${JAVA_OPT} -DconfPath=${PROXY_HOME}/conf"
JAVA_OPT="${JAVA_OPT} -Dlog4j2.AsyncQueueFullPolicy=Discard"
JAVA_OPT="${JAVA_OPT} -Drocketmq.client.logUseSlf4j=true"

#if [ -f "pid.file" ]; then
#        pid=`cat pid.file`
#        if ! is_proxyRunning "$pid"; then
#            echo "el proxy ya esta corriendo"
#            exit 9;
#        else
#	    echo "err pid$pid, rm pid.file"
#            rm pid.file
#        fi
#fi

pid=$(get_pid)
if [ -n "$pid" ];then
	echo -e "ERROR\t el servidor ya esta corriendo (pid=$pid), ya no es necesario ejecutar de nuevo start.sh"
	exit 9;
fi

make_logs_dir

echo "Utilizando jdk[$JAVA]" >> ${PROXY_LOG_HOME}/proxy.out


PROXY_MAIN=com.gcote.eventmanager.server.boot.ProxyStartup
$JAVA $JAVA_OPT -classpath ${PROXY_HOME}/conf:${PROXY_HOME}/apps/*:${PROXY_HOME}/lib/* $PROXY_MAIN >> ${PROXY_LOG_HOME}/proxy.out 2>&1 &
echo $!>pid.file
exit 0
