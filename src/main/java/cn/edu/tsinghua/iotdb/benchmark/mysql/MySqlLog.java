package cn.edu.tsinghua.iotdb.benchmark.mysql;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.tsinghua.iotdb.benchmark.conf.Config;
import cn.edu.tsinghua.iotdb.benchmark.conf.ConfigDescriptor;
import cn.edu.tsinghua.iotdb.benchmark.conf.Constants;
import cn.edu.tsinghua.iotdb.benchmark.db.InfluxDBFactory;
import cn.edu.tsinghua.iotdb.benchmark.db.IoTDBFactory;

public class MySqlLog {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(MySqlLog.class);
	private final String SAVE_CONFIG = "insert into CONFIG values(NULL, %s, %s, %s)";
	private final String SAVE_RESULT = "insert into RESULT values(NULL, %s, %s,%s)";
	private Connection mysqlConnection = null;
	private Config config = ConfigDescriptor.getInstance().getConfig();
	private String localName = "";
	private long labID;
	private String day = "";
	private String projectID = "";

	public MySqlLog() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			localName = localhost.getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			LOGGER.error("获取本机主机名称失败;UnknownHostException：{}", e.getMessage());
			e.printStackTrace();
		}
		localName = localName.replace("-", "_");
		localName = localName.replace(".", "_");
	}

	public void initMysql(long labIndex) {
		labID = labIndex;
		projectID = config.REMARK + labID;
		if (config.IS_USE_MYSQL) {
			Date date = new Date(labID);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
			day = sdf.format(date);
			try {
				Class.forName(Constants.MYSQL_DRIVENAME);
				mysqlConnection = DriverManager.getConnection(config.MYSQL_URL);
				initTable();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				LOGGER.error("mysql 初始化失败，原因是：{}", e.getMessage());
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				LOGGER.error("mysql 连接初始化失败，原因是：{}", e.getMessage());
				e.printStackTrace();
			}
		}

	}

	// 检查记录本次实验的表格是否已经创建，没有则创建
	public void initTable() {
		Statement stat = null;
		try {
			stat = mysqlConnection.createStatement();
			if (config.SERVER_MODE) {
				if (!hasTable("SERVER_MODE_" + localName + "_" + day)) {
					stat.executeUpdate("create table SERVER_MODE_"
							+ localName
							+ "_"
							+ day
							+ "(id BIGINT, cpu_usage DOUBLE,"
							+ "mem_usage DOUBLE,diskIo_usage DOUBLE,net_recv_rate DOUBLE,net_send_rate DOUBLE, remark varchar(6000),primary key(id))"
							+ " DEFAULT CHARSET=utf8");
					LOGGER.info("Table SERVER_MODE create success!");
				}
				return;
			}
			switch (config.DB_SWITCH) {
			case Constants.DB_IOT:
				if (!hasTable("IOTDB_DATA_MODEL" + "_" + day)) {
					stat.executeUpdate("create table IOTDB_DATA_MODEL"
							+ "_"
							+ day
							+ " (id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, projectID VARCHAR(150), sensor VARCHAR(50) NOT NULL, path VARCHAR(600))AUTO_INCREMENT = 1 DEFAULT CHARSET=utf8;");
					LOGGER.info("Table IOTDB_DATA_MODEL_{} create success!",
							day);
				}
				break;
			case Constants.DB_INFLUX:
				int i = 0,
				groupId = 0;
				if (!hasTable("INFLUXDB_DATA_MODEL" + "_" + day)) {
					stat.executeUpdate("create table INFLUXDB_DATA_MODEL"
							+ "_"
							+ day
							+ " (id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, projectID VARCHAR(150), measurement VARCHAR(50), tag VARCHAR(100), field VARCHAR(100))AUTO_INCREMENT = 1 DEFAULT CHARSET=utf8;");
					LOGGER.info("Table INFLUXDB_DATA_MODEL_ create success!",
							day);
				}
				break;
			}

			if (!hasTable("CONFIG")) {
				stat.executeUpdate("create table CONFIG (id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, projectID VARCHAR(150), configuration_item VARCHAR(150), configuration_value VARCHAR(150))AUTO_INCREMENT = 1 DEFAULT CHARSET=utf8;");
				LOGGER.info("Table CONFIG create success!");
			}
			if (!hasTable("RESULT")) {
				stat.executeUpdate("create table RESULT (id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, projectID VARCHAR(150), result_key VARCHAR(150), result_value VARCHAR(150))AUTO_INCREMENT = 1 DEFAULT CHARSET=utf8;");
				LOGGER.info("Table RESULT create success!");
			}
			if (config.IS_QUERY_TEST && !hasTable(projectID)) {
				stat.executeUpdate("create table "
						+ projectID
						+ "(id BIGINT, clientName varchar(50), "
						+ "loopIndex INTEGER, point INTEGER, time DOUBLE, cur_rate DOUBLE, remark varchar(6000), primary key(id,clientName)) DEFAULT CHARSET=utf8");
				LOGGER.info("Table {} create success!", projectID);
			}
			if (!config.IS_QUERY_TEST && !hasTable(projectID)) {
				stat.executeUpdate("create table "
						+ projectID
						+ "(id BIGINT, clientName varchar(50), "
						+ "loopIndex INTEGER, costTime DOUBLE, totalTime DOUBLE, cur_rate DOUBLE, errorPoint BIGINT, remark varchar(6000),primary key(id,clientName)) DEFAULT CHARSET=utf8");
				LOGGER.info("Table {} create success!", projectID);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("mysql 创建表格失败，原因是：{}", e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (stat != null)
					stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// 将插入测试的中间结果存入数据库
	public void saveInsertProcess(int index, double costTime, double totalTime,
			long errorPoint, String remark) {
		if (config.IS_USE_MYSQL) {
			String mysqlSql = String.format("insert into " + config.REMARK
					+ labID + " values(%d,%s,%d,%f,%f,%f,%d,%s)",
					System.currentTimeMillis(), "'"
							+ Thread.currentThread().getName() + "'", index,
					costTime, totalTime, (config.CACHE_NUM
							* config.SENSOR_NUMBER / costTime), errorPoint, "'"
							+ remark + "'");
			Statement stat;
			try {
				stat = mysqlConnection.createStatement();
				stat.executeUpdate(mysqlSql);
				stat.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOGGER.error(
						"{} save queryProcess info into mysql failed! Error：{}",
						Thread.currentThread().getName(), e.getMessage());
				LOGGER.error("{}", mysqlSql);
				e.printStackTrace();
			}
		}
	}

	// 将查询测试的过程数据存入mysql
	public void saveQueryProcess(int index, int point, double time,
			String remark) {
		double rate;
		if (config.IS_USE_MYSQL) {
			if (time == 0) {
				remark = "rate无效,because time = 0";
				rate = -1;
			} else {
				rate = point / time;
			}
			String mysqlSql = String.format("insert into " + config.REMARK
					+ labID + " values(%d,%s,%d,%d,%f,%f,%s)",
					System.currentTimeMillis(), "'"
							+ Thread.currentThread().getName() + "'", index,
					point, time, rate, "'" + remark + "'");
			Statement stat;
			try {
				stat = mysqlConnection.createStatement();
				stat.executeUpdate(mysqlSql);
				stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				LOGGER.error(
						"{} save queryProcess info into mysql failed! Error:{}",
						Thread.currentThread().getName(), e.getMessage());
				LOGGER.error("{}", mysqlSql);
				e.printStackTrace();
			}
		}
	}

	// 将系统资源利用信息存入mysql
	public void insertSERVER_MODE(double cpu, double mem, double io,
			double net_recv, double net_send, String remark) {
		if (config.IS_USE_MYSQL) {
			Statement stat = null;
			String sql = "";
			try {
				stat = mysqlConnection.createStatement();
				sql = String.format("insert into SERVER_MODE_" + localName
						+ "_" + day + " values(%d,%f,%f,%f,%f,%f,%s)",
						System.currentTimeMillis(), cpu, mem, io, net_recv,
						net_send, "'" + remark + "'");
				stat.executeUpdate(sql);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				LOGGER.error("{}将SERVER_MODE写入mysql失败,because:{}", sql,
						e.getMessage());
				e.printStackTrace();
			} finally {
				if (stat != null) {
					try {
						stat.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	// 存储IOTDB实验模型
	public void saveIoTDBDataModel(String sensor, String path) {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		Statement stat = null;
		String sql = "";
		try {
			stat = mysqlConnection.createStatement();
			sql = String.format("insert into IOTDB_DATA_MODEL" + "_" + day
					+ " values(NULL, %s, %s, %s)", "'" + projectID + "'", "'"
					+ sensor + "'", "'" + path + "'");
			stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}将结果信息写入mysql失败，because ：{}", sql, e.getMessage());
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// 存储IOTDB实验模型
	public void saveInfluxDBDataModel(String measurement, String tag,
			String field) {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		Statement stat = null;
		String sql = "";
		try {
			stat = mysqlConnection.createStatement();
			sql = String
					.format("insert into INFLUXDB_DATA_MODEL" + "_" + day
							+ " values(NULL, %s, %s, %s, %s)", "'" + projectID
							+ "'", "'" + measurement + "'", "'" + tag + "'",
							"'" + field + "'");
			stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}将结果信息写入mysql失败，because ：{}", sql, e.getMessage());
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// 存储实验结果
	public void saveResult(String k, String v) {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		Statement stat = null;
		String sql = "";
		try {
			stat = mysqlConnection.createStatement();
			sql = String.format(SAVE_RESULT, "'" + projectID + "'", "'" + k
					+ "'", "'" + v + "'");
			stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}将结果信息写入mysql失败，because ：{}", sql, e.getMessage());
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	// 存储实验配置信息
	public void saveConfig(String k, String v) {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		Statement stat = null;
		String sql = "";
		try {
			stat = mysqlConnection.createStatement();
			sql = String.format(SAVE_CONFIG, "'" + projectID + "'", "'" + k
					+ "'", "'" + v + "'");
			stat.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}将配置信息写入mysql失败，because ：{}", sql, e.getMessage());
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public void saveTestModel() throws SQLException {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		switch (config.DB_SWITCH) {
		case Constants.DB_IOT:
			for (String d : config.DEVICE_CODES) {
				for (String s : config.SENSOR_CODES) {
					this.saveIoTDBDataModel(d + "." + s,
							getFullGroupDevicePathByName(d) + "." + s);
				}
			}
			break;
		case Constants.DB_INFLUX:
			int i = 0,
			groupId = 0;
			for (String d : config.DEVICE_CODES) {
				for (String s : config.SENSOR_CODES) {
					this.saveInfluxDBDataModel("group_" + groupId, "'"
							+ "device=" + d + "'", "'" + s + "'");
				}
				i++;
				if (i % config.GROUP_NUMBER == 0) {
					groupId++;
				}
			}
			break;
		default:
			throw new SQLException("unsupported database " + config.DB_SWITCH);
		}

	}

	private String getFullGroupDevicePathByName(String d) {
		String[] spl = d.split("_");
		int id = Integer.parseInt(spl[1]);
		int groupSize = config.DEVICE_NUMBER / config.GROUP_NUMBER;
		int groupIndex = id / groupSize;
		return Constants.ROOT_SERIES_NAME + ".group_" + groupIndex + "."
				+ config.DEVICE_CODES.get(id);
	}

	public void savaTestConfig() {
		if (!config.IS_USE_MYSQL) {
			return;
		}
		Statement stat = null;
		String sql = "";
		try {
			stat = mysqlConnection.createStatement();
			sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
					"'DB_SWITCH'", "'" + config.DB_SWITCH + "'");
			stat.addBatch(sql);
			sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
					"'VERSION'", "'" + config.VERSION + "'");
			stat.addBatch(sql);
			sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
					"'CLIENT_NUMBER'", "'" + config.CLIENT_NUMBER + "'");
			stat.addBatch(sql);
			sql = String.format(SAVE_CONFIG, "'" + projectID + "'", "'LOOP'",
					"'" + config.LOOP + "'");
			stat.addBatch(sql);
			if (config.IS_QUERY_TEST) {// 查询测试
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'QUERY_CHOICE'",
						"'" + Constants.QUERY_CHOICE_NAME[config.QUERY_CHOICE]
								+ "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'QUERY_DEVICE_NUM'", "'" + config.QUERY_DEVICE_NUM
								+ "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'QUERY_SENSOR_NUM'", "'" + config.QUERY_SENSOR_NUM
								+ "'");
				stat.addBatch(sql);
				switch (config.QUERY_CHOICE) {
				case 1:
					sql = String
							.format(SAVE_CONFIG, "'" + projectID + "'",
									"'IS_RESULTSET_NULL'",
									"'" + config.IS_EMPTY_PRECISE_POINT_QUERY
											+ "'");
					stat.addBatch(sql);
				case 3:
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'QUERY_AGGREGATE_FUN'", "'"
									+ config.QUERY_AGGREGATE_FUN + "'");
					stat.addBatch(sql);
					break;
				case 4:
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'TIME_INTERVAL'", "'" + config.QUERY_INTERVAL
									+ "'");
					stat.addBatch(sql);
					break;
				case 5:
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'FILTRATION_CONDITION'", "'values > "
									+ config.QUERY_LOWER_LIMIT + "'");
					stat.addBatch(sql);
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'TIME_INTERVAL'", "'" + config.QUERY_INTERVAL
									+ "'");
					stat.addBatch(sql);
					break;
				case 7:
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'FILTRATION_CONDITION'", "'values > "
									+ config.QUERY_LOWER_LIMIT + "'");
					stat.addBatch(sql);
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'TIME_INTERVAL'", "'" + config.QUERY_INTERVAL
									+ "'");
					stat.addBatch(sql);
					sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
							"'TIME_UNIT'", "' " + config.TIME_UNIT + "'");
					stat.addBatch(sql);
					break;
				}
			} else {// 写入测试
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'GROUP_NUMBER'", "'" + config.GROUP_NUMBER + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'DEVICE_NUMBER'", "'" + config.DEVICE_NUMBER + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'SENSOR_NUMBER'", "'" + config.SENSOR_NUMBER + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'CACHE_NUM'", "'" + config.CACHE_NUM + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'POINT_STEP'", "'" + config.POINT_STEP + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'ENCODING'", "'" + config.ENCODING + "'");
				stat.addBatch(sql);
				sql = String.format(SAVE_CONFIG, "'" + projectID + "'",
						"'MUL_DEV_BATCH'", "'" + config.MUL_DEV_BATCH + "'");
				stat.addBatch(sql);
			}
			stat.executeBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			LOGGER.error("{}将配置信息写入mysql失败，because ：{}", sql, e.getMessage());
			e.printStackTrace();
		} finally {
			if (stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void closeMysql() {
		if (config.IS_USE_MYSQL) {
			if (mysqlConnection != null) {
				try {
					mysqlConnection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					LOGGER.error("mysql 连接关闭失败,原因是：{}", e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/** 数据库中是否已经存在名字为table的表 */
	private Boolean hasTable(String table) throws SQLException {
		String checkTable = "show tables like \"" + table + "\"";
		Statement stmt = mysqlConnection.createStatement();

		ResultSet resultSet = stmt.executeQuery(checkTable);
		if (resultSet.next()) {
			return true;
		} else {
			return false;
		}
	}

	public Connection getMysqlConnection() {
		return mysqlConnection;
	}

	public String getLocalName() {
		return localName;
	}

	public long getLabID() {
		return labID;
	}

	public void setLabID(long labID) {
		this.labID = labID;
	}
}
