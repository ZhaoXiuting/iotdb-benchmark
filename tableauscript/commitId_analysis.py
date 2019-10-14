from sqlalchemy import create_engine
import pandas as pd
import argparse

parser = argparse.ArgumentParser(description='Generate analysis result of query test.')
parser.add_argument('--mysql_host', '-a', default='192.168.130.19', help='mysql server address')
parser.add_argument('--mysql_database', '-d', default='weekly_test', help='mysql database')
args = parser.parse_args()
host = args.mysql_host
database = args.mysql_database
p_threshold=0.05
port=3306
user='root'
passwd='Ise_Nel_2017'
table_name = "gitLog"
file_name = "gitLog.txt"


def save_commitId():
    commit_id = read_commitId()
    engine = create_engine('mysql+pymysql://' + user + ':' + passwd + '@' + host + ':' + str(port) + '/' + database)
    df = pd.DataFrame({'commitid':commit_id})
    df.to_sql(table_name,engine,if_exists='append',index=False)


def read_commitId():
    engine = create_engine('mysql+pymysql://' + user + ':' + passwd + '@' + host + ':' + str(port) + '/' + database)
    sql = 'select commitId from ' + table_name
    commitId_df = pd.read_sql_query(sql, engine)
    commitId_df_list = commitId_df["commitId"].tolist()
    commitId_list = get_commitId()
    same_list = [x for x in commitId_df_list if x in commitId_list]
    difference_list = [y for y in (commitId_df_list + commitId_list) if y not in same_list]
    # difference_list = list(set(commitId_list)-set(commitId_df_list))
    return difference_list


def get_commitId():
    with open(file_name, "r", encoding="utf-8") as f:
        lines = f.readlines()
        line_list = []
        commit_id = []
        for i in range(len(lines)):
            line_list.append(lines[i].strip().split(" "))
        for j in range(len(line_list)):
            # print(line_list[j])
            pr = line_list[j][-1]
            if(pr.find("(#")==0):
                commit_id.append(line_list[j][0])
        return commit_id


def main():
    save_commitId()


if __name__ == '__main__':
    main()