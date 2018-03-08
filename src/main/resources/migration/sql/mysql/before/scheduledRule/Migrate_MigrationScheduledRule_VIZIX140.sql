DROP TABLE IF EXISTS `scheduledrule`;

CREATE TABLE `scheduledrule`
(
  id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ID of scheduledrule',
  active bit(1) COMMENT 'If scheduled Rule is active',
  code VARCHAR(255) ,
  cron_description VARCHAR(255) COMMENT 'cron description',
  description VARCHAR(255),
  name VARCHAR(255),
  rule_execution_mode VARCHAR(255),
  status VARCHAR(255),
  group_id BIGINT(20) COMMENT 'The ID reference to group0 table',
  edgebox_id BIGINT(20) COMMENT 'The ID reference to edgebox table',
  reportDefinition_id BIGINT(20) COMMENT 'The ID reference to reportDefinition table',
  CONSTRAINT FK_scheduled_group FOREIGN KEY (group_id) REFERENCES group0 (id),
  CONSTRAINT FK_scheduled_edgebox FOREIGN KEY (edgebox_id) REFERENCES edgebox (id),
  CONSTRAINT FK_scheduled_reportDefinition FOREIGN KEY (reportDefinition_id) REFERENCES reportDefinition (id)
);

ALTER TABLE `edgeboxrule`
ADD `scheduledRule_id` bigint(20) DEFAULT NULL,
ADD CONSTRAINT FK_edgeboxrule_scheduled FOREIGN KEY(scheduledRule_id) REFERENCES scheduledrule(id);