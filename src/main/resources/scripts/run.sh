#!/bin/sh

set -e -h

echo -n "Please input the listening port (1024-65535): "
read PORT

if [ $PORT -lt 1024 ] && [ $PORT -gt 65535 ]
then
  echo "Bad port - $PORT !"
  echo "Please choose one in range of (1024-65535)"
  exit 1
fi

echo -n "Please input the Scheduler period seconds (> 0): "
read PERIOD

if [ $PERIOD -lt 0 ]
then
  echo "Bad period - $PERIOD !"
  echo "Period must be greater than 0"
  exit 1
fi

echo "[`date`] Drip Task Server Start ..." >> logs/drip_task_server.log
echo "[`date`] Drip Task Server Start ..." >> logs/drip_task_server.err
nohup sh startup.sh $PORT $PERIOD >> logs/drip_task_server.log 2>>logs/drip_task_server.err &

sleep 5
echo "info" | nc localhost $PORT
