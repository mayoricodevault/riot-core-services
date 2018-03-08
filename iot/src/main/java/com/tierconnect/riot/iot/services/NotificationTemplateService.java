package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.NotificationTemplate;
import com.tierconnect.riot.sdk.dao.UserException;

import javax.annotation.Generated;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class NotificationTemplateService extends NotificationTemplateServiceBase 
{
    @Override public void validateInsert(NotificationTemplate notificationTemplate) {
        super.validateInsert(notificationTemplate);
        if (notificationTemplate.getId() == null && getNotificationTemplateDAO()
            .selectBy("templateName", notificationTemplate.getTemplateName()) != null){
            throw new UserException("Notification Template already exists.");
        }
    }

    @Override public void validateUpdate(NotificationTemplate notificationTemplate) {
        super.validateUpdate(notificationTemplate);
        if (notificationTemplate.getId() == null && getNotificationTemplateDAO()
            .selectBy("templateName", notificationTemplate.getTemplateName()) != null){
            throw new UserException("Notification Template already exists.");
        }
    }
}

