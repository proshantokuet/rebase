package org.opensrp.connector.openmrs.service.it;

import java.io.IOException;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensrp.connector.openmrs.service.OpenmrsUserService;
import org.opensrp.connector.openmrs.service.TestResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-applicationContext-opensrp-connector.xml")
public class OpenmrsUserServiceTest extends TestResourceLoader {
	
	public OpenmrsUserServiceTest() throws IOException {
		super();
		// TODO Auto-generated constructor stub
	}
	
	@Autowired
	private OpenmrsUserService openmrsUserService;
	
	@Before
	public void setup() {
		
	}
	
	@Test
	public void testGetUser() throws JSONException {
		Assert.assertEquals(openmrsUsername, openmrsUserService.getUser(openmrsUsername).getUsername());
		Assert.assertNotSame("admin", openmrsUserService.getUser(openmrsUsername).getUsername());
	}
	
	@Ignore
	@Test
	public void testNullPointerExceptionForGetUser() throws JSONException {
		openmrsUserService.getUser("openmrsUsername786").getUsername();
	}
	
	@Test
	public void testCreateProvider() throws JSONException {
		openmrsUserService.createProvider(openmrsUsername, "ZEIR_ID ");
		//Assert.assertEquals(expected, actual)
		
	}
	
}
