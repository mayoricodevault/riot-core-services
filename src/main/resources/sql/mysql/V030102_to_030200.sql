ALTER TABLE reportdefinition ADD bulkEdit bit(1) DEFAULT FALSE;

CREATE TABLE `notification_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `templateBody` longtext,
  `templateName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

ALTER TABLE apc_reportGroupBy ADD unit varchar(50) DEFAULT NULL;

ALTER TABLE connection0 ADD COLUMN code VARCHAR(255) DEFAULT NULL;

UPDATE riot_main.reportentryoption a
   SET a.group_id = (SELECT b.group_id
                      FROM riot_main.reportdefinition b
							WHERE b.id = a.reportDefinition_id);
