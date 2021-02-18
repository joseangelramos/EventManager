#!/bin/sh

#detect operating system.
OS=$(uname -o)

PROXY_HOME=`cd "./.." && pwd`

export PROXY_HOME

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

pid=$(get_pid)
if [ -z "$pid" ];then
	echo -e "El proxy no esta corriendo.."
	exit 0;
fi

kill ${pid}
echo "Enviando solicitud de apagado al proxy(${pid}) OK"

[[ $OS =~ Msys ]] && PS_PARAM=" -W "
stop_timeout=60
for no in $(seq 1 $stop_timeout); do
	if ps $PS_PARAM -p "$pid" 2>&1 > /dev/null; then
		if [ $no -lt $stop_timeout ]; then
			echo "[$no] apagando servidor ..."
			sleep 1
			continue
		fi

		echo "forzando apagado del servidor timeout, kill process: $pid"
		kill -9 $pid; sleep 1; break;
		echo "`date +'%Y-%m-%-d %H:%M:%S'` , pid : [$pid] , mensaje error: apagado anormal el cual no ha podido finalizar en 60s" > ../logs/shutdown.error
	else
		echo "servidor apagado ok!"; break;
	fi
done

if [ -f "pid.file" ]; then
    rm pid.file
fi


