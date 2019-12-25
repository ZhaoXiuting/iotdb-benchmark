#!/bin/bash
Host=192.168.130.19
User=root
PW=Ise_Nel_2017
sql=" SELECT commitId from gitLog WHERE STATUS = 'F' "
#echo $sql
result=`mysql -h 192.168.130.19 -P3306 -uroot -pIse_Nel_2017 commit_test -e "${sql}"`
#result=`mysql -h $Host -u $User -pweekly_test <<EOF
#SELECT commitId from gitLog WHERE STATUS = 'F'
#EOF`
#echo ${result}>test.txt
#m=$(awk 'END{print NR}' test.txt)
#echo
#if [ -z "${result}" ];then
    echo "没有搜索到状态为F的版本号"
else
    for i in $result
    do
    echo $i >> commitId.txt
    done
secommit=secommit_id
number=2
#number=$(awk 'END{print NR}' commitId.txt)
echo -e "\033[32m------------------------\033[1m"
#echo $number
commitid=$(awk "NR==$number""{print $1}" commitId.txt)
echo $commitid
sed -i 's/'$secommit'/'$commitid'/g' cli-benchmark.sh
#jia zhixingchaxunde jiaobencichu
echo "commitId is $commitid"
echo -e "\033[32m------------------------\033[0m"
source ./ciscripts/fit/ingestion-overflow50-auto-test.sh
sed -i 's/'$commitid'/'$secommit'/g' cli-benchmark.sh
rm -rf commitId.txt
fi
for i in $result
do
  if [ -z "${i}" ];then
    echo "没有搜索到状态为F的版本号"
  else
    echo $i >> commitId.txt
	secommit=secommit_id
    number=2
#number=$(awk 'END{print NR}' commitId.txt)
    echo -e "\033[32m------------------------\033[1m"
#echo $number
    commitid=$(awk "NR==$number""{print $1}" commitId.txt)
    echo $commitid
    sed -i 's/'$secommit'/'$commitid'/g' cli-benchmark.sh
    echo "commitId is $commitid"
    echo -e "\033[32m------------------------\033[0m"
    source ./ciscripts/fit/ingestion-overflow50-auto-test.sh
    sed -i 's/'$commitid'/'$secommit'/g' cli-benchmark.sh
    rm -rf commitId.txt
done
