#!/bin/bash
cd ../incubator-iotdb
Host=192.168.130.19
User=root
PW=Ise_Nel_2017
sql=" SELECT commitId from gitLog WHERE STATUS = 'F' "
#echo $sql
result=` mysql -h 192.168.130.19 -P3306 -uroot -pIse_Nel_2017 commit_test -e "${sql}" `
#result=`mysql -h $Host -u $User -pweekly_test <<EOF
#SELECT commitId from gitLog WHERE STATUS = 'F'
#EOF`
#echo ${result}>test.txt
#m=$(awk 'END{print NR}' test.txt)
#echo
for i in $result
do
echo $i >> commitId.txt
done
#cat commitId.txt
secommit=secommit_id
number=2
#number=$(awk 'END{print NR}' commitId.txt)
echo -e "\033[32m------------------------\033[1m"
#echo $number
commitid=$(awk "NR==$number""{print $1}" commitId.txt)
echo $commitid
git reset --hard $commitid
rm -rf commitId.txt
