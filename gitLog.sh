#!/bin/bash
cd ../incubator-iotdb
git log --pretty=format:"%H - %s" --since=10.weeks>../iotdb-benchmark/tableauscript/gitLog.txt
cd ../iotdb-benchmark/tableauscript
cat gitLog.txt
