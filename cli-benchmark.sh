#!/bin/sh

if [ -z "${BENCHMARK_HOME}" ]; then
  export BENCHMARK_HOME="$(cd "`dirname "$0"`"/.; pwd)"
fi

#configure the related path and host name
IOTDB_HOME=/home/liurui/github/iotdb/iotdb/bin
REMOTE_BENCHMARK_HOME=/home/liurui/github/iotdb-benchmark
HOST_NAME=liurui

#extract parameters from config.properties
IP=$(grep "HOST" $BENCHMARK_HOME/conf/config.properties)
FLAG_AND_DATA_PATH=$(grep "LOG_STOP_FLAG_PATH" $BENCHMARK_HOME/conf/config.properties)
SERVER_HOST=$HOST_NAME@${IP#*=}
LOG_STOP_FLAG_PATH=${FLAG_AND_DATA_PATH#*=}

#change to client mode
sed -i 's/SERVER_MODE *= *true/SERVER_MODE=false/g' $BENCHMARK_HOME/conf/config.properties
DB=$(grep "DB_SWITCH" $BENCHMARK_HOME/conf/config.properties)
QUERY_MODE=$(grep "IS_QUERY_TEST" $BENCHMARK_HOME/conf/config.properties)
GEN_DATA_MODE=$(grep "IS_GEN_DATA" $BENCHMARK_HOME/conf/config.properties)
echo Testing ${DB#*=} ...

mvn clean package -Dmaven.test.skip=true

#synchronize config server benchmark
#ssh -p 1309 $SERVER_HOST "rm $REMOTE_BENCHMARK_HOME/conf/config.properties"
#scp -P 1309 $BENCHMARK_HOME/conf/config.properties $SERVER_HOST:$REMOTE_BENCHMARK_HOME/conf

ssh $SERVER_HOST "rm $REMOTE_BENCHMARK_HOME/conf/config.properties"
scp $BENCHMARK_HOME/conf/config.properties $SERVER_HOST:$REMOTE_BENCHMARK_HOME/conf
scp $BENCHMARK_HOME/logs/lastPeriodResult.txt $SERVER_HOST:$REMOTE_BENCHMARK_HOME/logs

#start server system information recording
#ssh -p 1309 $SERVER_HOST "sh $REMOTE_BENCHMARK_HOME/ser-benchmark.sh > /dev/null 2>&1 &"

ssh $SERVER_HOST "sh $REMOTE_BENCHMARK_HOME/ser-benchmark.sh > /dev/null 2>&1 &"

if [ "${DB#*=}" = "IoTDB" -a "${QUERY_MODE#*=}" = "false" ]; then
  echo "initial database in server..."
  #ssh -p 1309 $SERVER_HOST "cd $LOG_STOP_FLAG_PATH;rm -rf data;sh $IOTDB_HOME/stop-server.sh;sleep 2"
  #ssh -p 1309 $SERVER_HOST "cd $LOG_STOP_FLAG_PATH;sh $IOTDB_HOME/start-server.sh > /dev/null 2>&1 &"
  ssh $SERVER_HOST "cd $LOG_STOP_FLAG_PATH;rm -rf data;sh $IOTDB_HOME/stop-server.sh;sleep 2"
  ssh $SERVER_HOST "cd $LOG_STOP_FLAG_PATH;sh $IOTDB_HOME/start-server.sh > /dev/null 2>&1 &"
  echo 'wait a few seconds for lauching IoTDB...'
  sleep 20
fi

echo '------Client Test Begin Time------'
date
cd bin
sh startup.sh -cf ../conf/config.properties
wait

#stop server system information recording
#ssh -p 1309 $SERVER_HOST "touch $LOG_STOP_FLAG_PATH/log_stop_flag"

ssh $SERVER_HOST "touch $LOG_STOP_FLAG_PATH/log_stop_flag"

#ssh $SERVER_HOST "grep Statistic $LOG_STOP_FLAG_PATH/logs/log_info.log | tail -n 1 " >> $BENCHMARK_HOME/logs/MemoryMonitor.log


if [ "${DB#*=}" = "IoTDB" -a "${QUERY_MODE#*=}" = "false" ]; then
    sleep 5
    wait
    ssh $SERVER_HOST "tail -n 2 $REMOTE_BENCHMARK_HOME/logs/log_result_info.txt" >> $BENCHMARK_HOME/logs/log_info.log
    ssh $SERVER_HOST "tail -n 2 $REMOTE_BENCHMARK_HOME/logs/log_result_info.txt" >> $BENCHMARK_HOME/logs/log_result_info.txt
fi

echo '------Client Test Complete Time------'
date

