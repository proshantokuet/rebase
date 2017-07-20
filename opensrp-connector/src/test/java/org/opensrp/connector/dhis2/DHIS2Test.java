package org.opensrp.connector.dhis2;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opensrp.connector.dhis2.DHIS2AggregateConnector;
import org.opensrp.connector.dhis2.Dhis2TrackCaptureConnector;



public class DHIS2Test{
	
	
	public DHIS2Test() throws IOException {
		super();
	}
	
	@Before
	public void setup(){
		
	}
	
	@Ignore@Test
	public void shouldCreateAggregateData() throws JSONException {
		DHIS2AggregateConnector dhis2 = new DHIS2AggregateConnector("http://192.168.19.17:8080/api/", "path", "Path@123");
		JSONObject aggregateData = dhis2.getAggregateDataCount();
		System.out.println(aggregateData.toString());
		System.out.println(dhis2.aggredateDataSendToDHIS2(aggregateData));
		
		
	}
	 
	@Ignore @Test
	public void shouldCreateTrackCaptureData() throws JSONException, ParseException {		
		Dhis2TrackCaptureConnector dhis2 = new Dhis2TrackCaptureConnector("http://192.168.19.17:8080/api/", "path", "Path@123");	
		JSONObject track = dhis2.getTrackCaptureFromFormSubmission();
		System.out.println(dhis2.trackCaptureDataSendToDHIS2(track));
			
	}
	@Test
	public void DatesTest(){
		Date date = new Date();
		String modifiedDate= new SimpleDateFormat("yyyy-MM-dd").format(date);
		
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MONTH, -1);
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH)+1;
		
		int length = (int)(Math.log10(month)+1);
		String formatted ;
		System.out.println(length);
		if(length<2){
			System.out.println("okkk");
		 formatted = String.format("%02d", month);
		}else{
			formatted =Integer.toString(month);
		}
		
		
		System.out.println(""+formatted);
	}
}
