package AWS_Retriever;

import java.sql.ResultSet;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.List;
import java.lang.Object;
import java.util.ArrayList;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.*;

import java.util.*;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AWS_Retriever {

	public static String AWS_HOST = "https://api.smartwater.ml";
	public static String SQL_HOST = "HOST";
	public static String SQL_USERNAME = "USERNAME";
	public static String SQL_USERPASS = "PASSWORD";
	public static String SQL_DB = "DB";
	public static int SQL_PORT = 3306;
	
	public static AWS_RESTful_Interface aws;
	public static MySQL_Interface sql;
	
	public static DateTimeFormatter GeneralDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	public static DateTimeFormatter ISO8601DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	private static volatile ScheduledFuture<?> result;
	public static Timer[] timers;
	
	public static void main(String[] args) throws Exception {
		if ( args.length != 0 ) {
			AWS_HOST = args[0];
			SQL_HOST = args[1];
			SQL_USERNAME = args[2];
			SQL_USERPASS = args[3];
			SQL_DB = args[4];
			SQL_PORT = Integer.parseInt(args[5]);
		}
		
		aws = new AWS_RESTful_Interface(AWS_HOST);
		sql = new MySQL_Interface(SQL_HOST, SQL_USERNAME, SQL_USERPASS, SQL_DB, SQL_PORT);
		
    	try {
    		
    		try {
    			sql.setupConnection();
    			syncDatabase();
    			sql.closeConnection();
    		} catch (Exception e) {
				e.printStackTrace();
			}
    		
    		scheduleUpdates_today();
    		
    		while(true);
    		
    	}catch (Exception e) {
            e.printStackTrace();
        }finally {
        	System.out.println("Closing Program");
        	sql.closeConnection();
        	System.out.println("Program Closed");
        }
		
	}
	
	public static void syncDatabase() throws Exception {
		
		String description = "";
		String description_temp;
		String operationTime = Instant.now().toString();
		
		sql.createUpdateLogTable();
		
		String prevTimeStamp;
		boolean success = false;
		try {
			prevTimeStamp = sql.getLastUpdateTime();
		} catch (Exception e) {
			prevTimeStamp = "1970-01-01T00:00:01.000Z";
		}
		
		description_temp = "Syncing Database: " + dateformatFromISO( operationTime ) + "; ";
		description += description_temp; System.out.println(description_temp);
		
		try {
			description_temp = syncDatabase_getFlow(prevTimeStamp,operationTime);
			description += description_temp;
			
			description_temp = syncDatabase_getFlush(prevTimeStamp,operationTime);
			description += description_temp;
			
			description_temp = syncDatabase_getHealth(prevTimeStamp,operationTime);
			description += description_temp;
			
			success = true;
			
		} catch (Exception e) {
			description_temp = "Something went wrong at this point";
			description += description_temp; System.out.println(description_temp);
            e.printStackTrace();
        }finally {
        	syncDatabase_setUpdateLog(operationTime, description, success);
        }
		
		System.out.println("\nOperation Complete\n");
		
	}
	
	public static String syncDatabase_getFlow(String prevTimeStamp, String operationTime) throws Exception {
		//Get flow events
		JSONObject flowEventsResponse = aws.getFlowEvents();
		if (flowEventsResponse.getBoolean("success")) {
			JSONArray flowEvents = flowEventsResponse.getJSONArray("events");
			
			System.out.println("Checking flow tables...");
			ArrayList<String> flowSensors = new ArrayList<String>();
			for (int i = 0; i < flowEvents.length(); i++) {
				JSONObject flowEvent = flowEvents.getJSONObject(i);
				if (!flowSensors.contains(flowEvent.getString("device"))) {
					flowSensors.add(flowEvent.getString("device"));
				}
			}
			for (int i = 0; i < flowSensors.size(); i++) {
				sql.addBatchStatement(sql.createFlowTableStatement(flowSensors.get(i)));
			}	
			
			System.out.print("Storing new flow entries - 000%");
			for (int i = 0; i < flowEvents.length(); i++) {
				System.out.print("\b\b\b\b");
				JSONObject flowEvent = flowEvents.getJSONObject(i);
				
				String statement = "INSERT INTO `" + flowEvent.getString("device") + "` (id_cloud, inserted_local , inserted_cloud, timestamp, duration, count, raw) " + 
						"SELECT * FROM (SELECT " +
						"'" + flowEvent.getString("_id") + "' AS id_cloud, " +
						"'" + dateformatFromISO( operationTime ) + "' AS inserted_local, " +
						"'" + dateformatFromISO( flowEvent.getString("inserted") ) + "' AS inserted_cloud, " +
						"'" + dateformatFromISO( flowEvent.getString("timestamp") ) + "' AS timestamp, " +
						flowEvent.getInt("duration") + " AS duration, " +
						flowEvent.getInt("count") + " AS count, " +
						"'" + flowEvent.getString("raw") + "' AS raw) ";
				statement += "AS query WHERE NOT EXISTS (" + 
						"SELECT id_cloud FROM `" + flowEvent.getString("device") + "` WHERE (" +
						"id_cloud = '" + flowEvent.getString("_id") + "' AND " +
						"timestamp = '" + dateformatFromISO( flowEvent.getString("timestamp") ) + "')" +
						") LIMIT 1;";
				
				int progress = i*100/flowEvents.length();
				System.out.print((String.format("%03d", progress))+"%");
				sql.executeUpdateQueryStatement(statement);
			}
			sql.executeBatchStatement();
			System.out.println("\b\b\b\b100%");
			return ("Flow entries on AWS: " + flowEvents.length() + "; ");
		}
		return "Something went wrong";
	}
	
	
	public static String syncDatabase_getFlush(String prevTimeStamp, String operationTime) throws Exception {
		//Get flush events
		JSONObject flushEventsResponse = aws.getFlushEvents();
		if (flushEventsResponse.getBoolean("success")) {
			JSONArray flushEvents = flushEventsResponse.getJSONArray("events");
			
			System.out.println("Checking flush tables...");
			ArrayList<String> flushSensors = new ArrayList<String>();
			for (int i = 0; i < flushEvents.length(); i++) {
				JSONObject flushEvent = flushEvents.getJSONObject(i);
				if (!flushSensors.contains(flushEvent.getString("device"))) {
					flushSensors.add(flushEvent.getString("device"));
				}
			}
			for (int i = 0; i < flushSensors.size(); i++) {
				sql.addBatchStatement(sql.createFlushTableStatement(flushSensors.get(i)));
			}
			
			System.out.print("Storing new flush entries - 000%");
			for (int i = 0; i < flushEvents.length(); i++) {
				System.out.print("\b\b\b\b");
				JSONObject flushEvent = flushEvents.getJSONObject(i);
				
				String statement = "INSERT INTO `" + flushEvent.getString("device") + "` (id_cloud, inserted_local , inserted_cloud, timestamp, duration) " + 
						"SELECT * FROM (SELECT " +
						"'" + flushEvent.getString("_id") + "' AS id_cloud, " +
						"'" + dateformatFromISO( operationTime ) + "' AS inserted_local, " +
						"'" + dateformatFromISO( flushEvent.getString("inserted") ) + "' AS inserted_cloud, " +
						"'" + dateformatFromISO( flushEvent.getString("timestamp") ) + "' AS timestamp, " +
						flushEvent.getInt("duration") + " AS duration) ";
				statement += "AS query WHERE NOT EXISTS (" + 
						"SELECT id_cloud FROM `" + flushEvent.getString("device") + "` WHERE (" +
						"id_cloud = '" + flushEvent.getString("_id") + "' AND " +
						"timestamp = '" + dateformatFromISO( flushEvent.getString("timestamp") ) + "')" +
						") LIMIT 1;";
				
				int progress = i*100/flushEvents.length();
				System.out.print((String.format("%03d", progress))+"%");
				sql.executeUpdateQueryStatement(statement);
			}
			sql.executeBatchStatement();
			System.out.println("\b\b\b\b100%");
			return ("Flush entries on AWS: " + flushEvents.length() + "; ");
		}
		return "Something went wrong";
	}
	
	public static String syncDatabase_getHealth(String prevTimeStamp, String operationTime) throws Exception {
		//Get health reports
		JSONObject healthReportsResponse = aws.getHealth();
		if (healthReportsResponse.getBoolean("success")) {
			JSONArray healthReports = healthReportsResponse.getJSONArray("reports");
			
			System.out.println("Checking health report tables...");
			ArrayList<String> healthReportSensors = new ArrayList<String>();
			for (int i = 0; i < healthReports.length(); i++) {
				JSONObject healthReport = healthReports.getJSONObject(i);
				if (!healthReportSensors.contains(healthReport.getString("device"))) {
					healthReportSensors.add(healthReport.getString("device"));
				}
			}
			for (int i = 0; i < healthReportSensors.size(); i++) {
				sql.addBatchStatement(sql.createHealthTableStatement(healthReportSensors.get(i)+"_health"));
			}
			
			System.out.print("Storing new health reports - 000%");
			for (int i = 0; i < healthReports.length(); i++) {
				System.out.print("\b\b\b\b");
				JSONObject healthReport = healthReports.getJSONObject(i);
				String statement = "INSERT INTO `" + healthReport.getString("device") + "_health` (id_cloud, inserted_local , inserted_cloud, timestamp, battery, voltage, capacity) " + 
						"SELECT * FROM (SELECT " +
						"'" + healthReport.getString("_id") + "' AS id_cloud, " +
						"'" + dateformatFromISO( operationTime ) + "' AS inserted_local, " +
						"'" + dateformatFromISO( healthReport.getString("inserted") ) + "' AS inserted_cloud, " +
						"'" + dateformatFromISO( healthReport.getString("timestamp") ) + "' AS timestamp, " +
						healthReport.getBoolean("battery") + " AS battery, " +
						healthReport.getInt("voltage") + " AS voltage, " +
						healthReport.getInt("capacity") + " AS capacity) ";
				statement += "AS query WHERE NOT EXISTS (" + 
						"SELECT id_cloud FROM `" + healthReport.getString("device") + "_health` WHERE (" +
						"id_cloud = '" + healthReport.getString("_id") + "' AND " +
						"timestamp = '" + dateformatFromISO( healthReport.getString("timestamp") ) + "')" +
						") LIMIT 1;";
				
				int progress = i*100/healthReports.length();
				System.out.print((String.format("%03d", progress))+"%");
				sql.executeUpdateQueryStatement(statement);
			}
			sql.executeBatchStatement();
			System.out.println("\b\b\b\b100%");
			return ("Health reports on AWS: " + healthReports.length() + "; ");
		}
		return "Something went wrong";
	}
	
	public static void syncDatabase_setUpdateLog(String timestamp, String description, boolean success) throws Exception {
		String update_log_stmt = "INSERT INTO update_log VALUES(" +
				"NULL" + ", " + 
				"'" + timestamp + "', ";
		if (success) {
			update_log_stmt += "true";
		} else {
			update_log_stmt += "false";
		}
		update_log_stmt += ", " + "'" + description + "')";
		sql.executeUpdateQueryStatement(update_log_stmt);
	}
	
	public static void scheduleUpdates_today() throws Exception{
		timers = new Timer[25];
		for (int i = 0; i < 24; i++) {
			Calendar calendar = Calendar.getInstance();
			if (i > calendar.get(Calendar.HOUR_OF_DAY)) {
				calendar.set(Calendar.HOUR_OF_DAY, i);
				calendar.set(Calendar.MINUTE, 1);
				calendar.set(Calendar.SECOND, 0);
				Date time = calendar.getTime();
				timers[i] = new Timer();
				timers[i].schedule(new ScheduledUpdate(),time);
			}
		}
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date time = calendar.getTime();
		timers[24] = new Timer();
		timers[24].schedule(new ScheduledUpdates(),time);
	}
	
	static class ScheduledUpdate extends TimerTask {
		public void run() {
			try {
				sql.setupConnection();
    			syncDatabase();
    			sql.closeConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static class ScheduledUpdates extends TimerTask {
		public void run() {
			timers = new Timer[25];
			for (int i = 0; i < 24; i++) {
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, i);
				calendar.set(Calendar.MINUTE, 1);
				calendar.set(Calendar.SECOND, 0);
				calendar.add(Calendar.DATE, 1);
				Date time = calendar.getTime();
				timers[i] = new Timer();
				timers[i].schedule(new ScheduledUpdate(),time);
			}
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.add(Calendar.DATE, 1);
			Date time = calendar.getTime();
			timers[24] = new Timer();
			timers[24].schedule(new ScheduledUpdates(),time);
		}
	}
	
	public static String dateformatFromISO(String datetime) throws Exception {
		String dateUTC = datetime;
	    Instant instant = Instant.parse(dateUTC);
	    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Asia/Hong_Kong"));
	    return zonedDateTime.format(GeneralDateFormat);
    }
	
	public static String dateformatToISO(String datetime) throws Exception {
		DateFormat generalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		DateFormat ISO8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String dateUTC = ISO8601Format.format( generalFormat.parse( datetime ) ); 
	    Instant instant = Instant.parse(dateUTC);
	    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Pacific/Pitcairn"));
	    return zonedDateTime.format(ISO8601DateFormat);
    }
	
}