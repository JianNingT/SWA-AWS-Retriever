package AWS_Retriever;

import java.sql.*;
import java.util.Properties;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MySQL_Interface {
	
	private String HOST;
	private String USERNAME;
	private String USERPASS;
	private String DB;
	private int PORT;
	
	private Connection con = null;
	private Statement batch_statement = null;
	private int batch_statement_size = 0;
	private Integer batchLock = new Integer(1);
	
	public MySQL_Interface(String host, String username, String password, String db, int port) {
		HOST = host;
		USERNAME = username;
		USERPASS = password;
		DB = db;
		PORT = port;
	}
	
	public void setupConnection() throws Exception {
    	String driver = "com.mysql.cj.jdbc.Driver";
    	String url = "jdbc:mysql://" + HOST + ":" + PORT + "/";
    	
    	Properties props = new Properties();
		props.setProperty("user",USERNAME);
		props.setProperty("password",USERPASS);
    	
		Class.forName(driver);
		con = DriverManager.getConnection(url, USERNAME, USERPASS);
        		
    	executeUpdateStatement("CREATE DATABASE IF NOT EXISTS " + DB + ";");
    	executeUpdateStatement("use " + DB + ";");
    	
        System.out.println("MySQL Connection Setup: Complete");
	}
	public void closeConnection() throws Exception {
		con.close();
		System.out.println("MySQL Connection Closed");
	}
	
	public ResultSet executeQueryStatement(String statement) throws Exception {
		executeBatchStatement();
        PreparedStatement ps = con.prepareStatement(statement);
        return ps.executeQuery();
    }
    public void executeUpdateStatement(String statement) throws Exception {
    	executeBatchStatement();
		Statement st = con.createStatement();
		st.executeUpdate(statement);
	}
    public void executeUpdateQueryStatement(String statement) throws Exception {
    	executeBatchStatement();
		PreparedStatement ps = con.prepareStatement(statement);
		ps.executeUpdate();
	}
    public void addBatchStatement(String statement) throws Exception {
    	synchronized(this.batchLock) {
    		if (batch_statement == null) {
        		con.setAutoCommit(false);
        		batch_statement = con.createStatement();
        	}
	    	batch_statement.addBatch(statement);
	    	batch_statement_size+=1;
	    	if (batch_statement_size >= 100) {
	    		executeBatchStatement();
	    	}
    	}
	}
	public void executeBatchStatement() throws Exception {
    	if (batch_statement != null) {
    		batch_statement.executeBatch();
	        con.commit();
	        batch_statement_size = 0;
	        batch_statement = null;
	        con.setAutoCommit(true);
    	}
	}
	
	public boolean databaseEmpty() throws Exception {
    	ResultSet rs = executeQueryStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + DB + "';");
		rs.first();
		return ( rs.getInt(1) == 0 );
    }
    
	public void createDeviceTable() throws Exception {
    	String statement = 
				"CREATE TABLE IF NOT EXISTS devices (" +
			    "id INT AUTO_INCREMENT," +
			    "device_id TINYTEXT," +
			    "registered_local DATETIME(3)," +
				"registered_cloud DATETIME(3)," +
				"type VARCHAR(10)," +
				"location TEXT," +
				"active TINYINT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
		executeUpdateStatement(statement);
    }
    public void createFlowTable(String tableName) throws Exception {
		executeUpdateStatement( createFlowTableStatement(tableName) );
    }
    public String createFlowTableStatement(String tableName) {
    	return "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
			    "id INT AUTO_INCREMENT," +
			    "id_cloud TEXT," +
			    "inserted_local DATETIME(3)," +
			    "inserted_cloud DATETIME(3)," +
				"timestamp DATETIME(3)," +
				"duration INT," +
				"count INT," +
				"raw LONGTEXT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
    }
    public void createFlushTable(String tableName) throws Exception {
		executeUpdateStatement( createFlushTableStatement(tableName) );
    }
    public String createFlushTableStatement(String tableName) {
    	return "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
			    "id INT AUTO_INCREMENT," +
			    "id_cloud TEXT," +
			    "inserted_local DATETIME(3)," +
			    "inserted_cloud DATETIME(3)," +
				"timestamp DATETIME(3)," +
				"duration INT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
    }
    public void createHealthTable(String tableName) throws Exception {
		executeUpdateStatement( createHealthTableStatement(tableName) );
    }
    public String createHealthTableStatement(String tableName) {
    	return "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
			    "id INT AUTO_INCREMENT," +
			    "id_cloud TEXT," +
			    "inserted_local DATETIME(3)," +
			    "inserted_cloud DATETIME(3)," +
				"timestamp DATETIME(3)," +
				"battery BOOLEAN," +
				"voltage INT," +
				"capacity INT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
    }
    public void createUpdateLogTable() throws Exception {
    	String statement = 
				"CREATE TABLE IF NOT EXISTS update_log (" +
			    "id INT AUTO_INCREMENT," +
				"timestamp TINYTEXT," +
				"success BOOLEAN," +
				"description TEXT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
		executeUpdateStatement(statement);
    }
    public void createUnregisteredEntriesTable() throws Exception {
    	String statement = 
				"CREATE TABLE IF NOT EXISTS unregistered_entries (" +
			    "id INT AUTO_INCREMENT," +
				"entry LONGTEXT," +
				"PRIMARY KEY (id)" +
				")  ENGINE=INNODB;";
		executeUpdateStatement(statement);
    }
    
    public String getLastUpdateTime() throws Exception {
    	ResultSet rs = executeQueryStatement("SELECT * FROM update_log WHERE success=true ORDER BY id DESC LIMIT 1");
    	String output = "1970-01-01T00:00:01.000Z";
		rs.beforeFirst();
    	while (rs.next()) {
      	   output = rs.getString("timestamp");
      	}
    	
    	return output;
    	
    }
    
}