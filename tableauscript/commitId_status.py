from sqlalchemy import create_engine
import pandas as pd
import argparse

parser = argparse.ArgumentParser(description='Generate analysis result of query test.')
parser.add_argument('--mysql_host', '-a', default='192.168.130.19', help='mysql server address')
parser.add_argument('--mysql_database', '-d', default='commit_test', help='mysql database')
args = parser.parse_args()
host = args.mysql_host
database = args.mysql_database
p_threshold=0.05
port=3306
user='root'
passwd='Ise_Nel_2017'
table_name = "gitLog"
config_table_name = "versionResult"


def read_commitId():
    engine = create_engine('mysql+pymysql://' + user + ':' + passwd + '@' + host + ':' + str(port) + '/' + database)
    sql = 'select version from ' + config_table_name
    version_df = pd.read_sql_query(sql, engine)
    version_list = version_df["version"].tolist()
    commitId_list = []
    for version in version_list:
        if version.find("commit_id:")!=-1:
            commitId = version.strip().split(":")[1]
            if commitId!="" and commitId!=None:
                commitId_list.append(commitId)
    return list(set(commitId_list))


def update_commitId():
    engine = create_engine('mysql+pymysql://' + user + ':' + passwd + '@' + host + ':' + str(port) + '/' + database)
    version_list =  read_commitId()
    for version in version_list:
        update_sql = "update "+table_name+" set status='T'  WHERE commitId='"+version+"'"
        print(update_sql)
        engine.execute(update_sql)


def main():
    update_commitId()


if __name__ == '__main__':
    main()