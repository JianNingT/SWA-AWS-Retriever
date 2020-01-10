package AWS_Retriever;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

//Interface for SWA Express Backend @ https://github.com/smart-water-auditing/express-backend
//API reference @ https://github.com/smart-water-auditing/express-backend/blob/master/api/README.md#devices-api

public class AWS_RESTful_Interface {
	
	public String HOST;
	
	public AWS_RESTful_Interface(String host) {
		HOST = host;
	}
	
	public JSONObject sendGet(String input) throws Exception {
		String url = HOST+input;
		
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();
		
		BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return new JSONObject(response.toString());
		
	}
	
	public JSONObject getDevices(String registered) throws Exception {
		return sendGet("/devices" + "?registered=" + registered );
	}
	
	public JSONObject getFlowEvents() throws Exception {
		return sendGet("/flow");
	}
	
	public JSONObject getFlowEvents(String inserted) throws Exception {
		return sendGet("/flow" + "?inserted=" + inserted );
	}

	public JSONObject getFlushEvents() throws Exception {
		return sendGet("/flush");
	}
	
	public JSONObject getFlushEvents(String inserted) throws Exception {
		return sendGet("/flush" + "?inserted=" + inserted );
	}
	
	public JSONObject getHealth() throws Exception {
		return sendGet("/health");
	}
	
	public JSONObject getHealth(String inserted) throws Exception {
		return sendGet("/health" + "?inserted=" + inserted );
	}
	
}