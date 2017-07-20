package org.opensrp.service.formSubmission.handler;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

import java.util.List;

import org.opensrp.form.domain.FormSubmission;
import org.opensrp.form.service.FormSubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FormSubmissionRouter {
	private static Logger logger = LoggerFactory.getLogger(FormSubmissionRouter.class.toString());
	private FormSubmissionService formSubmissionService;
	private HandlerMapper handlerMapper;

	@Autowired
	public FormSubmissionRouter(FormSubmissionService formSubmissionService, HandlerMapper handlerMapper) {

		this.formSubmissionService = formSubmissionService;
		this.handlerMapper = handlerMapper;
	}
	
	public void formSubmissionProcessed(String client, List<String> dependents, FormSubmission formSubmission) {
		FormSubmissionProcessedListener handler = handlerMapper.formSubmissionProcessedListenerMap().get(formSubmission.formName());
		if(handler != null){
			logger.info(format("Found a post processor handler for form submission ( {0} ) with instance Id: {1} for entity: {2}",
					formSubmission.formName(), formSubmission.instanceId(), formSubmission.entityId()));
			handler.onFormSubmissionProcessed(client, dependents, formSubmission);
		}
	}

	public void route(String instanceId) throws Exception {
		FormSubmission submission = formSubmissionService.findByInstanceId(instanceId);
		route(submission);
	}
	
	public void route(FormSubmission formSubmission) throws Exception {
		CustomFormSubmissionHandler handler = handlerMapper.customFormSubmissionHandlerMap().get(formSubmission.formName());// handlerMap.get(submission.formName());
		if (handler == null) {
			logger.warn(format("Could not find a handler due to unknown form submission ( {0} ) with instance Id: {1} for entity: {2}",
				formSubmission.formName(), formSubmission.instanceId(), formSubmission.entityId()));
			return;
		}
		logger.info(format("Handling {0} form submission with instance Id: {1} for entity: {2}",
				formSubmission.formName(), formSubmission.instanceId(), formSubmission.entityId()));
		try {
			handler.handle(formSubmission);
		} catch (Exception e) {
			logger.error(format("Handling {0} form submission with instance Id: {1} for entity: {2} failed with exception : {3}",
					formSubmission.formName(), formSubmission.instanceId(), formSubmission.entityId(), getFullStackTrace(e)));
			throw e;
		}
	}
}
