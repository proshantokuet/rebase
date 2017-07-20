package org.opensrp.web.controller;

import org.opensrp.dto.Action;
import org.opensrp.dto.ActionData;

public class ActionConvertor {
    public static Action from(org.opensrp.scheduler.Action action){
        return new Action(action.baseEntityId(), action.target(), action.actionType(), action.data(), String.valueOf(action.timestamp()), action.getIsActionActive(), action.details());
    }

    public static org.opensrp.scheduler.Action toAction(Action actionItem, String anmIdentifier) {
        return new org.opensrp.scheduler.Action(actionItem.caseID(), anmIdentifier, ActionData.from(actionItem.type(), actionItem.target(), actionItem.data(), actionItem.details()));
    }
}
