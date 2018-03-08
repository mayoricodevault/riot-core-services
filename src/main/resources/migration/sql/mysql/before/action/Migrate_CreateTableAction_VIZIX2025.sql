DROP TABLE IF EXISTS `actionconfiguration`;

CREATE TABLE `actionconfiguration`
(
  id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ID of action',
  code VARCHAR(20) NOT NULL COMMENT 'The code of the action',
  configuration LONGTEXT COMMENT 'The configuration in format JSON',
  name VARCHAR(150) NOT NULL COMMENT 'The name of the action',
  status VARCHAR(20) COMMENT 'The status of the action: ACTIVE, DELETED',
  type VARCHAR(100) NOT NULL COMMENT 'The type of the action: HTTP',
  group_id BIGINT(20) NOT NULL COMMENT 'The ID reference to group0 table',
  CONSTRAINT FK_action_group FOREIGN KEY (group_id) REFERENCES group0 (id)
);

DROP TABLE IF EXISTS `logexecutionaction`;

CREATE TABLE `logexecutionaction`
(
  id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ID of log',
  endDate DATETIME COMMENT 'The date when the call was completed in MODAL mode',
  iniDate DATETIME NOT NULL COMMENT 'The date when the call was initialized',
  processTime BIGINT(20) COMMENT 'The duration time of the call',
  request LONGTEXT COMMENT 'The configuration send',
  response LONGTEXT COMMENT 'Http response',
  responseCode VARCHAR(20) COMMENT 'The code http of the call',
  actionConfiguration_id BIGINT(20) NOT NULL COMMENT 'The ID reference to action configuration table',
  createdByUser_id BIGINT(20) NOT NULL COMMENT 'The ID reference to user0 table',
  CONSTRAINT FK_log_action FOREIGN KEY (actionConfiguration_id) REFERENCES actionconfiguration (id),
  CONSTRAINT FK_log_user FOREIGN KEY (createdByUser_id) REFERENCES user0 (id)
);

DROP TABLE IF EXISTS `reportactions`;
CREATE TABLE `reportactions`
(
  id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ID of report action',
  displayOrder INT(11) NOT NULL COMMENT 'Display order of the action',
  actionConfiguration_id BIGINT(20) NOT NULL COMMENT 'The ID reference to action configuration table',
  createdByUser_id BIGINT(20) NOT NULL COMMENT 'The ID reference to user0 table',
  reportDefinition_id BIGINT(20) NOT NULL COMMENT 'The ID reference to report definition table',
  CONSTRAINT FK_report_user FOREIGN KEY (createdByUser_id) REFERENCES user0 (id),
  CONSTRAINT FK_report_action FOREIGN KEY (actionConfiguration_id) REFERENCES actionconfiguration (id),
  CONSTRAINT FK_report_reportdefinition FOREIGN KEY (reportDefinition_id) REFERENCES reportdefinition (id)
);