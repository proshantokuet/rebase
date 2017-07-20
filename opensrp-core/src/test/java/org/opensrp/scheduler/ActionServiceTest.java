/*package org.opensrp.scheduler;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opensrp.dto.AlertStatus.normal;
import static org.opensrp.dto.AlertStatus.urgent;
import static org.opensrp.dto.BeneficiaryType.child;
import static org.opensrp.dto.BeneficiaryType.ec;
import static org.opensrp.dto.BeneficiaryType.mother;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensrp.domain.BaseEntity;
import org.opensrp.dto.ActionData;
import org.opensrp.dto.MonthSummaryDatum;
import org.opensrp.scheduler.Action;
import org.opensrp.scheduler.repository.AllActions;
import org.opensrp.scheduler.repository.AllAlerts;
import org.opensrp.scheduler.service.ActionService;
import org.opensrp.service.BaseEntityService;

import com.google.gson.Gson;

public class ActionServiceTest {
    @Mock
    private AllActions allActions;
    
    @Mock 
    private AllAlerts allAlerts;
    
    @Mock
    private BaseEntityService baseEntityService;

    private ActionService service;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        allActions.removeAll();
        allAlerts.removeAll();
        service = new ActionService(allActions, allAlerts);
    }

    @Test
    public void shouldSaveAlertActionForEntity() throws Exception {
        DateTime dueDate = DateTime.now().minusDays(1);
        DateTime expiryDate = dueDate.plusWeeks(2);
        service.alertForBeneficiary("mother", "Case X", "ANM ID M", "Ante Natal Care - Normal", "ANC 1", normal, dueDate, expiryDate);

        Action action = new Action("Case X", "ANM ID M", ActionData.createAlert("mother", "Ante Natal Care - Normal", "ANC 1", normal, dueDate, expiryDate));
        verify(allActions).addOrUpdateAlert(action);
        verify(allActions).add(action);
        verify(allAlerts).addOrUpdateScheduleNotificationAlert("mother", "Case X", "ANM ID M", "Ante Natal Care - Normal", "ANC 1", normal, dueDate, expiryDate);
    }

    @Test
    public void shouldDeleteExistingAlertBeforeCreatingNewOne() throws Exception {
        DateTime dueDate = DateTime.now().minusDays(1);
        DateTime expiryDate = dueDate.plusWeeks(2);
        service.alertForBeneficiary(mother, "Case X", "ANM ID M", "Ante Natal Care - Normal", "ANC 1", normal, dueDate, expiryDate);

        verify(allActions).addOrUpdateAlert(new Action("Case X", "ANM ID M", ActionData.createAlert(mother, "Ante Natal Care - Normal", "ANC 1", normal, dueDate, expiryDate)));
    }

    @Test
    public void shouldSaveAlertActionForChild() throws Exception {
        when(allChildren.findByCaseId("Case X")).thenReturn(new Child("Case X", "MOTHER-CASE-1", "bcg", "3", "female").withAnm("ANM ID C"));

        DateTime dueDate = DateTime.now().minusDays(1);
        DateTime expiryDate = dueDate.plusWeeks(2);
        service.alertForBeneficiary(child, "Case X", "ANM ID C", "OPV", "OPV", normal, dueDate, expiryDate);

        verify(allActions).addOrUpdateAlert(new Action("Case X", "ANM ID C", ActionData.createAlert(child, "OPV", "OPV", normal, dueDate, expiryDate)));
    }

    @Test
    public void shouldSaveAlertActionForEC() throws Exception {
        when(allEligibleCouples.findByCaseId("Case X")).thenReturn(new EligibleCouple("Case X", "EC-CASE-1").withANMIdentifier("ANM ID C"));

        DateTime dueDate = DateTime.now().minusDays(1);
        DateTime expiryDate = dueDate.plusWeeks(2);
        service.alertForBeneficiary(ec, "Case X", "ANM ID C", "OCP Refill", "OCP Refill", urgent, dueDate, expiryDate);

        verify(allActions).addOrUpdateAlert(new Action("Case X", "ANM ID C", ActionData.createAlert(ec, "OCP Refill", "OCP Refill", urgent, dueDate, expiryDate)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBeneficiaryTypeIsUnknown() throws Exception {
        service.alertForBeneficiary(null, "Case X", "ANM ID C", "Schedule name", "FP Complications", urgent, null, null);
    }

    @Test
    public void shouldCreateACloseActionForAVisitOfAChild() throws Exception {
        service.markAlertAsClosed("Case X", "ANM 1", "OPV 1", "2012-01-01");

        verify(allActions).add(new Action("Case X", "ANM 1", ActionData.markAlertAsClosed("OPV 1", "2012-01-01")));
    }

    @Test
    public void shouldCreateACloseActionForAVisitOfAMother() throws Exception {
        when(allMothers.findByCaseId("Case X")).thenReturn(new Mother("Case X", "EC-CASE-1", "Thayi 1").withAnm("ANM ID 1"));

        service.markAlertAsClosed("Case X", "ANM ID 1", "ANC 1", "2012-12-12");

        verify(allActions).add(new Action("Case X", "ANM ID 1", ActionData.markAlertAsClosed("ANC 1", "2012-12-12")));
    }

    @Test
    public void shouldCreateADeleteAllActionForAMother() throws Exception {
        service.markAllAlertsAsInactive("Case X");

        verify(allActions).markAllAsInActiveFor("Case X");
    }

    @Test
    public void shouldMarkAnAlertAsInactive() throws Exception {
        service.markAlertAsInactive("ANM 1","Case X", "Schedule 1");

        verify(allActions).markAlertAsInactiveFor("ANM 1","Case X", "Schedule 1");
    }

    @Test
    public void shouldReturnAlertsBasedOnANMIDAndTimeStamp() throws Exception {
        List<Action> alertActions = Arrays.asList(new Action("Case X", "ANM 1", ActionData.createAlert(mother, "Ante Natal Care - Normal", "ANC 1", normal, DateTime.now(), DateTime.now().plusDays(3))));
        when(allActions.findByANMIDAndTimeStamp("ANM 1", 1010101)).thenReturn(alertActions);

        List<Action> alerts = service.getNewAlertsForANM("ANM 1", 1010101);

        assertEquals(1, alerts.size());
        assertEquals(alertActions, alerts);
    }

    @Test
    public void shouldReportForIndicator() {
        ActionData summaryActionData = ActionData.reportForIndicator("ANC", "30", new Gson().toJson(asList(new MonthSummaryDatum("3", "2012", "2", "2", asList("CASE 5", "CASE 6")))));

        service.reportForIndicator("ANM X", summaryActionData);

        verify(allActions).add(new Action("", "ANM X", summaryActionData));
    }

    @Test
    public void shouldDeleteAllActionsWithTargetReport() {
        service.deleteReportActions();

        verify(allActions).deleteAllByTarget("report");
    }
}
*/