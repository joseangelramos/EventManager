#!/bin/bash
set -e;

function usage(){
	echo "Uso: watchdog.sh [option]";
	echo -e " -h, --help \t Este texto de ayuda."
	echo -e " -a, --add-crontab \t agrega job watchdog a crontab."
	echo -e " -d, --delete-crontab \t elimina job wathcdog de crontab."
	echo -e " -w, --work\t ejecuta script watchdog UNA vez."
}

function add_crontab(){
	crontab -l | grep -v "$APP_HOME/bin/watchdog.sh" > tmp_crontab.txt || true
	mkdir -p $APP_HOME/logs/ && touch $APP_HOME/logs/watchdog.log
	echo "* * * * * $APP_HOME/bin/watchdog.sh -w >> $APP_HOME/logs/watchdog.log 2>&1" >> tmp_crontab.txt
	crontab tmp_crontab.txt
	rm -f tmp_crontab.txt
}

function delete_crontab(){
	crontab -l | grep -v "$APP_HOME/bin/watchdog.sh" > tmp_crontab.txt || true
	crontab tmp_crontab.txt
	rm -f tmp_crontab.txt
}

function restart_service(){
    echo "$(date) INFO bajando servicio ..."
	./stop.sh || { local code=$?; echo -e "$(date) ERROR\t fallo llamado stop.sh, code=$code."; exit $code; }

    echo "$(date) INFO iniciando servicio ..."
	./start.sh || { local code=$?; echo -e "$(date) ERROR\t fallo llamado start.sh, code=$code."; exit $code; }
	echo "$(date) INFO reiniciando servicio."
}

function work (){
	if [ ! -f "sys.pid" ]; then
	echo -e "$(date) ERROR\t Archivo sys.pid no encontrado, intentando reiniciar servicio."
		restart_service;
		exit $?;
	fi

	pid=$(cat sys.pid)
	if ps -fp ${pid} 2>&1 > /dev/null; then
		exit 0;
	else
		echo -e "$(date) ERROR\t proceso($pid) no encontrado, intentando reiniciar servicio."
		restart_service;
		exit $?;
	fi
}

## script starts here.
APP_BIN="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="$(dirname $APP_BIN)"; [ -d "$APP_HOME" ] || { echo "ERROR Mumble SDK Internal Bug, error al detectar APP_HOME."; exit 1;}
# parse command line.
cd ${APP_BIN};
OPTS=`getopt -o a::d::h::w:: --long add-crontab::,delete-crontab::,help::,work::  -- "$@"`
if [ $? != 0 ] ; then usage; exit 1 ; fi
eval set -- "$OPTS"
while true ; do
        case "$1" in
                -a|--add-crontab) add_crontab; exit $?;;
                -d|--delete-crontab) delete_crontab; exit $?;;
                -w|--work) work; exit $?;;
                -h|--help) usage; exit 0;;
                *) usage; exit 1 ;;
        esac
done
