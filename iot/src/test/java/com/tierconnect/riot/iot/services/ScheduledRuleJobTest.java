package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.ScheduledRule;
import org.junit.Test;

/**
 * Created by brayan on 6/27/17.
 */
public class ScheduledRuleJobTest {
    @Test
    public void executeScheduledAction(){
        ScheduledRuleService scheduledRuleService = new ScheduledRuleService();
        ScheduledRule scheduledRule = new ScheduledRule();
        scheduledRuleService.scheduledRuleJob(scheduledRule);
    }
}
