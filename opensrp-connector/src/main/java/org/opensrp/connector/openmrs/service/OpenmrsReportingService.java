package org.opensrp.connector.openmrs.service;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.common.util.HttpUtil;
import org.springframework.stereotype.Service;

@Service
public class OpenmrsReportingService extends OpenmrsService{
//	/private static final String REPORT_URL = "ws/rest/v1/reportingrest";
	private static final String REPORT_DEFINITION_URL = "ws/rest/v1/reportingrest/reportDefinition";
	private static final String REPORT_DATA_URL = "ws/rest/v1/reportingrest/reportdata";

	public OpenmrsReportingService() {
		
	}
	
	public OpenmrsReportingService(String openmrsUrl, String user, String password) {
    	super(openmrsUrl, user, password);
	}
	
	public JSONArray getReportDefinitions() throws JSONException {
		JSONArray res = new JSONObject(HttpUtil.get(getURL()
    			+"/"+REPORT_DEFINITION_URL, "v=full", OPENMRS_USER, OPENMRS_PWD).body())
    			.getJSONArray("results");
		
		for (int i = 0; i < res.length(); i++) {
			JSONObject jo = res.getJSONObject(i);
			jo.remove("links");
			jo.remove("resourceVersion");
			jo.remove("class");
		}
		return res;
	}
	
	public JSONArray getReportData(String uuid, Map<String, String> params) throws JSONException {
		String payload = "";
		if(params != null)
		for (Entry<String, String> e : params.entrySet()) {
			payload += e.getKey()+"="+e.getValue()+"&";
		}
		JSONArray ds = new JSONObject(HttpUtil.get(getURL()
    			+"/"+REPORT_DATA_URL+"/"+uuid, payload, 
    			OPENMRS_USER, OPENMRS_PWD).body())
    			.getJSONArray("dataSets");
		
		for (int i = 0; i < ds.length(); i++) {
			JSONObject jo = ds.getJSONObject(i);
			jo.remove("links");
			jo.remove("definition");
			jo.remove("resourceVersion");
			jo.remove("class");
		}
		return ds;
	}
}
