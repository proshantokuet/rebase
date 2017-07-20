package org.opensrp.connector.openmrs.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.common.util.HttpResponse;
import org.opensrp.common.util.HttpUtil;
import org.opensrp.domain.Client;
import org.opensrp.domain.Event;
import org.opensrp.domain.Obs;
import org.opensrp.domain.User;
import org.opensrp.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mysql.jdbc.StringUtils;

@Service
public class EncounterService extends OpenmrsService{
	private static final String ENCOUNTER_URL = "ws/rest/v1/encounter";//"ws/rest/emrapi/encounter";
	private static final String OBS_URL = "ws/rest/v1/obs";
	private static final String ENCOUNTER__TYPE_URL = "ws/rest/v1/encountertype";
	public static final String OPENMRS_UUID_IDENTIFIER_TYPE = "OPENMRS_UUID";

	private PatientService patientService;
	private OpenmrsUserService userService;
	private ClientService clientService;

	@Autowired
	public EncounterService(PatientService patientService, OpenmrsUserService userService, ClientService clientService) {
		this.patientService = patientService;
		this.userService = userService;
		this.clientService = clientService;
	}
	
	public EncounterService(String openmrsUrl, String user, String password) {
    	super(openmrsUrl, user, password);
	}
	
	public PatientService getPatientService() {
		return patientService;
	}

	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}

	public OpenmrsUserService getUserService() {
		return userService;
	}

	public void setUserService(OpenmrsUserService userService) {
		this.userService = userService;
	}

    public JSONObject getEncounterByUuid(String uuid, boolean noRepresentationTag) throws JSONException {
    	return new JSONObject(HttpUtil.get(getURL()
    			+"/"+ENCOUNTER_URL+"/"+uuid, noRepresentationTag?"":"v=full", OPENMRS_USER, OPENMRS_PWD).body());
    }
    
    public JSONObject getObsByEncounterUuid(String encounterUuid) throws JSONException {
    	// The data format returned contains the obs uuid and concept uuids
    	return new JSONObject(HttpUtil.get(getURL()
    			+"/"+ENCOUNTER_URL+"/" + encounterUuid, "v=custom:(uuid,obs:(uuid,concept:(uuid)))", OPENMRS_USER, OPENMRS_PWD).body());
    }
    
    public JSONObject getObsUuidByParentObsUuid(String obsUuid) throws JSONException {
    	//The data format returned contains the children obs uuid and concept uuids
    	return new JSONObject(HttpUtil.get(getURL()
			+"/"+OBS_URL+"/" + obsUuid, "v=custom:(groupMembers:(uuid,concept:(uuid)))", OPENMRS_USER, OPENMRS_PWD).body());
    }
    
	public JSONObject getEncounterType(String encounterType) throws JSONException {
    	// we have to use this ugly approach because identifier not found throws exception and 
    	// its hard to find whether it was network error or object not found or server error
    	JSONArray res = new JSONObject(HttpUtil.get(getURL()+"/"+ENCOUNTER__TYPE_URL, "v=full", 
    			OPENMRS_USER, OPENMRS_PWD).body()).getJSONArray("results");
    	for (int i = 0; i < res.length(); i++) {
			if(res.getJSONObject(i).getString("display").equalsIgnoreCase(encounterType)){
				return res.getJSONObject(i);
			}
		}
    	return null;
    }
	
    public JSONObject createEncounterType(String name, String description) throws JSONException{
		JSONObject o = convertEncounterToOpenmrsJson(name, description);
		return new JSONObject(HttpUtil.post(getURL()+"/"+ENCOUNTER__TYPE_URL, "", o.toString(), OPENMRS_USER, OPENMRS_PWD).body());
	}
    
    public JSONObject convertEncounterToOpenmrsJson(String name, String description) throws JSONException {
		JSONObject a = new JSONObject();
		a.put("name", name);
		a.put("description", description);
		return a;
	}
	
	public JSONObject createEncounter(Event e) throws JSONException{
		JSONObject pt = patientService.getPatientByIdentifier(e.getBaseEntityId());
		JSONObject enc = new JSONObject();
		
		JSONObject pr = userService.getPersonByUser(e.getProviderId());
		
		enc.put("encounterDatetime", OPENMRS_DATE.format(e.getEventDate().toDate()));
		// patient must be existing in OpenMRS before it submits an encounter. if it doesnot it would throw NPE
		enc.put("patient", pt.getString("uuid"));
		//TODO enc.put("patientUuid", pt.getString("uuid"));
		enc.put("encounterType", e.getEventType());
		//TODO enc.put("encounterTypeUuid", e.getEventType());
		enc.put("location", e.getLocationId());
		enc.put("provider", pr.getString("uuid"));

		List<Obs> ol = e.getObs();
		Map<String, JSONArray> p = new HashMap<>();
		Map<String, JSONArray> pc = new HashMap<>();
		
		if(ol != null)
		for (Obs obs : ol) {
			if(!StringUtils.isEmptyOrWhitespaceOnly(obs.getFieldCode()) && (obs.getFieldType()==null||  obs.getFieldType().equalsIgnoreCase("concept"))){//skipping empty obs and fields that don't have concepts
				//if no parent simply make it root obs
				if(StringUtils.isEmptyOrWhitespaceOnly(obs.getParentCode())){
					p.put(obs.getFieldCode(), convertObsToJson(obs));
				}
				else {
					//find parent obs if not found search and fill or create one
					JSONArray parentObs = p.get(obs.getParentCode());
					if(parentObs == null){
						p.put(obs.getParentCode(), convertObsToJson(getOrCreateParent(ol, obs)));
					}
					// find if any other exists with same parent if so add to the list otherwise create new list
					JSONArray obl = pc.get(obs.getParentCode());
					if(obl == null){
						obl = new JSONArray();
					}
					JSONArray addobs = convertObsToJson(obs);
					for (int i = 0; i < addobs.length(); i++) {
						obl.put(addobs.getJSONObject(i));
					}
					pc.put(obs.getParentCode(), obl);
				}
			}
		}
		
		JSONArray obar = new JSONArray();
		for (String ok : p.keySet()) {
			for (int i = 0; i < p.get(ok).length(); i++) {
				JSONObject obo = p.get(ok).getJSONObject(i);
				
				JSONArray cob = pc.get(ok);
				if(cob != null && cob.length() > 0) {
					obo.put("groupMembers", cob);
				}
				
				obar.put(obo);
			}
		}
		enc.put("obs", obar);
		
		HttpResponse op = HttpUtil.post(HttpUtil.removeEndingSlash(OPENMRS_BASE_URL)+"/"+ENCOUNTER_URL, "", enc.toString(), OPENMRS_USER, OPENMRS_PWD);
		return new JSONObject(op.body());
	}
	
	public JSONObject buildUpdateEncounter(Event e) throws JSONException {
		String openmrsuuid = e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE);
		JSONObject encounterObsUuids = getObsByEncounterUuid(openmrsuuid);
		JSONArray obsUuids = encounterObsUuids.getJSONArray("obs");
		
		System.out.print("[OBS-UUIDS]" + obsUuids);
		

		JSONObject pt = patientService.getPatientByIdentifier(e.getBaseEntityId());//TODO find by any identifier
		JSONObject enc = new JSONObject();
		
		JSONObject pr = userService.getPersonByUser(e.getProviderId());
		
		enc.put("encounterDatetime", OPENMRS_DATE.format(e.getEventDate().toDate()));
		// patient must be existing in OpenMRS before it submits an encounter. if it doesnot it would throw NPE
		enc.put("patient", pt.getString("uuid"));
	//TODO	enc.put("patientUuid", pt.getString("uuid"));
		enc.put("encounterType", e.getEventType());
		enc.put("location", e.getLocationId());
		enc.put("provider", pr.has("uuid")?pr.getString("uuid"):"");

		List<Obs> ol = e.getObs();
		Map<String, JSONArray> p = new HashMap<>();
		Map<String, JSONArray> pc = new HashMap<>();
		
		if(ol != null)
		for (Obs obs : ol) {
			if(!StringUtils.isEmptyOrWhitespaceOnly(obs.getFieldCode()) &&(obs.getFieldType()==null|| obs.getFieldType().equalsIgnoreCase("concept"))){//skipping empty obs
				//if no parent simply make it root obs
				if(StringUtils.isEmptyOrWhitespaceOnly(obs.getParentCode())){
					p.put(obs.getFieldCode(), convertObsToJson(obs));
				}
				else {
					//find parent obs if not found search and fill or create one
					JSONArray parentObs = p.get(obs.getParentCode());
					if(parentObs == null) {
						p.put(obs.getParentCode(), convertObsToJson(getOrCreateParent(ol, obs)));
					}
					// find if any other exists with same parent if so add to the list otherwise create new list
					JSONArray obl = pc.get(obs.getParentCode());
					if(obl == null) {
						obl = new JSONArray();
					}
					JSONArray addobs = convertObsToJson(obs);
					for (int i = 0; i < addobs.length(); i++) {
						obl.put(addobs.getJSONObject(i));
					}
					pc.put(obs.getParentCode(), obl);
				}
			}
		}
		
		JSONArray obar = new JSONArray();
		for (String ok : p.keySet()) {
			for (int i = 0; i < p.get(ok).length(); i++) {
				JSONObject obo = p.get(ok).getJSONObject(i);
				obo.put("uuid", getObsUuid(obo, obsUuids));
				
				JSONArray cob = pc.get(ok);
				if(cob != null && cob.length() > 0) {
					// Fetch children obs uuids
					JSONObject obsGroupUuids = getObsUuidByParentObsUuid(obo.getString("uuid"));
					JSONArray groupUuids = obsGroupUuids.getJSONArray("groupMembers");
					// Add uuids to group members
					for(int j = 0; j < cob.length(); j++) {
						JSONObject cobObj = cob.getJSONObject(j);
						cobObj.put("uuid", getObsUuid(cobObj, groupUuids));
					}

					obo.put("groupMembers", cob);
				}
				
				obar.put(obo);
			}
		}
		enc.put("obs", obar);
		
		return enc;
	}
	
	public JSONObject updateEncounter(Event e) throws JSONException {
		if(StringUtils.isEmptyOrWhitespaceOnly(e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE))){
			throw new IllegalArgumentException("Encounter was never pushed to OpenMRS as "+OPENMRS_UUID_IDENTIFIER_TYPE+" is empty. Consider creating a new one");
		}
		
		String openmrsuuid = e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE);

		JSONObject enc = buildUpdateEncounter(e);
		
		HttpResponse op = HttpUtil.post(HttpUtil.removeEndingSlash(OPENMRS_BASE_URL)+"/"+ENCOUNTER_URL+"/"+openmrsuuid, "", enc.toString(), OPENMRS_USER, OPENMRS_PWD);
		return new JSONObject(op.body());
	}
	
	private String getObsUuid(JSONObject obs, JSONArray obsUuids) throws JSONException {
		String uuid = "";
		// obs = {"concept":"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
		// obsUuids = [{"concept":{"uuid":"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"},"uuid":"b267b2f5-94be-43e8-85c4-4e36f2eb8471"}, {}]

		for(int i = 0; i < obsUuids.length(); i++) {
			JSONObject obsUuid = obsUuids.getJSONObject(i);
			JSONObject conceptObj = obsUuid.getJSONObject("concept");
			
			if(conceptObj.get("uuid").equals(obs.get("concept"))) {
				return obsUuid.getString("uuid");
			}
		}
		
		return uuid;
	}
	
	private JSONArray convertObsToJson(Obs o) throws JSONException{
		JSONArray arr = new JSONArray();
		if(o.getValues() == null || o.getValues().size() == 0) {//must be parent of some obs
			JSONObject obo = new JSONObject();
			obo.put("concept", o.getFieldCode());

			arr.put(obo);
		}
		else {
			//OpenMRS can not handle multivalued obs so add obs with multiple values as two different obs
			for (Object v : o.getValues()) {
				JSONObject obo = new JSONObject();
				obo.put("concept", o.getFieldCode());
				obo.put("value", v);
	
				arr.put(obo);
			}
		}
		return arr;
	}
	
	private Obs getOrCreateParent(List<Obs> obl, Obs o){
		for (Obs obs : obl) {
			if(o.getParentCode().equalsIgnoreCase(obs.getFieldCode())){
				return obs;
			}
		}
		return new Obs("concept", "parent", o.getParentCode(), null, null, null, null);
	}
	// TODO needs review and refactor
	public Event convertToEvent(JSONObject encounter) throws JSONException{
		if(encounter.has("patient") == false){
			throw new IllegalStateException("No 'patient' object found in given encounter");
		}
		Event e = new Event();
		String patientUuid = encounter.getJSONObject("patient").getString("uuid");
		Client c = clientService.find(patientUuid);
		if(c == null || c.getBaseEntityId() == null){
			//try to get the client from openmrs based on the uuid
			JSONObject openmrsPatient = patientService.getPatientByUuid(patientUuid, false);
			c=patientService.convertToClient(openmrsPatient);
			if(c==null || c.getBaseEntityId() == null){
			throw new IllegalStateException("Client was not found registered while converting Encounter to an Event in OpenSRP");
			}else{
				clientService.addClient(c);
			}
		}
		
		JSONObject creator = encounter.getJSONObject("auditInfo").getJSONObject("creator");
		e.withBaseEntityId(c.getBaseEntityId())
			.withCreator(new User(creator.getString("uuid"), creator.getString("display"), null, null))
			.withDateCreated(DateTime.now());
		
		e.withEventDate(new DateTime(encounter.getString("encounterDatetime")))
			//.withEntityType(entityType) //TODO
			.withEventType(encounter.getJSONObject("encounterType").getString("name"))
			//.withFormSubmissionId(formSubmissionId)//TODO
			.withLocationId((encounter.has("location")&&encounter.get("location") instanceof JSONObject)?encounter.getJSONObject("location").getString("name"):"")
			//TODO manage providers and uuid in couch
			.withProviderId(creator.getString("display"))
			.withVoided(encounter.getBoolean("voided"));
		
		e.addIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE, encounter.getString("uuid"));
		
		JSONArray ol = encounter.getJSONArray("obs");
		for (int i = 0; i < ol.length(); i++) {
			JSONObject o = ol.getJSONObject(i);
			List<Object> values = new ArrayList<Object>();
			if(o.optJSONObject("value") != null){
				values.add(o.getJSONObject("value").getString("uuid"));
			}
			else if(o.has("value")){
				values.add(o.getString("value"));
			}
			e.addObs(new Obs(null, null, o.getJSONObject("concept").getString("uuid"), null /*//TODO handle parent*/, values, null/*comments*/, null/*formSubmissionField*/));
		}
		
		return e;
	}
}
