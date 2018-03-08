package com.tierconnect.riot.iot.services;

import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ActionConfiguration;
import com.tierconnect.riot.iot.entities.QReportActions;
import com.tierconnect.riot.iot.entities.ReportActions;

import java.util.List;

public class ReportActionsService extends ReportActionsServiceBase {

    public ReportActions getReportActionByActionIDandReportID(Long actionID, Long reportID) {
        BooleanExpression expression = QReportActions.reportActions.reportDefinition.id.eq(reportID);
        expression.and(QReportActions.reportActions.actionConfiguration.id.eq(actionID));
        expression = expression.and(QReportActions.reportActions.actionConfiguration.status.ne(Constants.ACTION_STATUS_DELETED));
        List<ReportActions> reportActionsList = getReportActionsDAO().getQuery().where(expression)
                .orderBy(QReportActions.reportActions.id.desc()).list(QReportActions.reportActions);
        if (reportActionsList != null && !reportActionsList.isEmpty()) {
            return reportActionsList.get(0);
        }
        return null;
    }

    public List<ReportActions> getReportActionsActives(Long reportDefinitionId, ActionConfiguration actionConfiguration) {
        BooleanExpression expression = QReportActions.reportActions.reportDefinition.id.ne(reportDefinitionId);
        expression = expression.and(QReportActions.reportActions.actionConfiguration.status.ne(Constants.ACTION_STATUS_DELETED));
        expression = expression.and(QReportActions.reportActions.actionConfiguration.id.eq(actionConfiguration.getId()));
        return getReportActionsDAO().getQuery().where(expression).list(QReportActions.reportActions);
    }

    public List<ReportActions> getReportActionsActives(Long reportDefinitionId) {
        BooleanExpression expression = QReportActions.reportActions.reportDefinition.id.eq(reportDefinitionId);
        expression = expression.and(QReportActions.reportActions.actionConfiguration.status.ne(Constants.ACTION_STATUS_DELETED));
        return getReportActionsDAO().getQuery().where(expression).list(QReportActions.reportActions);
    }

    public List<ReportActions> getReportActions(Long reportDefinitionId) {
        return getReportActionsDAO().getQuery()
                .where(QReportActions.reportActions.reportDefinition.id.eq(reportDefinitionId))
                .list(QReportActions.reportActions);
    }
}

