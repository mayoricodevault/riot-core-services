CREATE TABLE `license` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `installTime` datetime DEFAULT NULL,
  `licenseString` longtext,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_mestjo4nxmg6surk9f8lcr9tb` (`group_id`),
  CONSTRAINT `FK_mestjo4nxmg6surk9f8lcr9tb` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`)
) ENGINE=InnoDB;
------------------------------------------------------------------
ALTER TABLE reportDefinition
 ADD COLUMN verticalTotal bit(1) AFTER shift_id
 , ADD COLUMN horizontalTotal bit(1) AFTER shift_id;

ALTER TABLE reportDefinition ADD timeoutCache int(11) DEFAULT NULL;
ALTER TABLE reportDefinition ADD zoneOpacity int(11) DEFAULT NULL;
ALTER TABLE reportDefinition ADD mapOpacity int(11) DEFAULT NULL;

SET SQL_SAFE_UPDATES = 0;

 UPDATE reportDefinition a
   SET a.horizontalTotal = 1
	  ,a.verticalTotal = 1
 WHERE a.reporttype = 'tableSummary'
   AND a.horizontalTotal IS NULL
   AND a.verticalTotal IS NULL;

CREATE TABLE IF NOT EXISTS `reportEntryOption` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `RFIDPrint` bit(1) DEFAULT NULL,
  `associate` bit(1) DEFAULT NULL,
  `defaultRFIDPrint` bigint(20) DEFAULT NULL,
  `defaultZPLTemplate` bigint(20) DEFAULT NULL,
  `deleteOption` bit(1) DEFAULT NULL,
  `disassociate` bit(1) DEFAULT NULL,
  `displayOrder` float DEFAULT NULL,
  `editOption` bit(1) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `newOption` bit(1) DEFAULT NULL,
  `reportDefinition_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_s6rdxd0fj3sjpqov7ikuh3vwa` (`reportDefinition_id`),
  CONSTRAINT `FK_s6rdxd0fj3sjpqov7ikuh3vwa` FOREIGN KEY (`reportDefinition_id`) REFERENCES `reportDefinition` (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS  `reportEntryOptionProperty` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `displayOrder` float DEFAULT NULL,
  `editInline` bit(1) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `pickList` bit(1) DEFAULT NULL,
  `propertyName` varchar(255) DEFAULT NULL,
  `required` bit(1) DEFAULT NULL,
  `sortBy` varchar(255) DEFAULT NULL,
  `thingTypeFieldId` bigint(20) DEFAULT NULL,
  `thingTypeIdReport` bigint(20) DEFAULT NULL,
  `reportEntryOption_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_lilv604yhupnscoy76kmyo7ec` (`reportEntryOption_id`),
  CONSTRAINT `FK_lilv604yhupnscoy76kmyo7ec` FOREIGN KEY (`reportEntryOption_id`) REFERENCES `reportEntryOption` (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS  `entryFormPropertyData` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `reportEntryOptionProperty_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_3sguvgyv2b1g36xdyfl71w3tj` (`reportEntryOptionProperty_id`),
  CONSTRAINT `FK_3sguvgyv2b1g36xdyfl71w3tj` FOREIGN KEY (`reportEntryOptionProperty_id`) REFERENCES `reportEntryOptionProperty` (`id`)
) ENGINE=InnoDB;
---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS  `dataType` (
  `id` bigint(20) NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `entity` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS  `thingTypeTemplate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `pathIcon` varchar(255) DEFAULT NULL,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_91dv2dqjcbe88rmie5akb7651` (`group_id`),
  CONSTRAINT `FK_91dv2dqjcbe88rmie5akb7651` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `thingTypeFieldTemplate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `defaultValue` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `symbol` varchar(255) DEFAULT NULL,
  `type` bigint(20) DEFAULT NULL,
  `typeParent` varchar(255) DEFAULT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `thingTypeTemplate_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_c8mn3no4e4ga6nomeju90gf87` (`thingTypeTemplate_id`),
  CONSTRAINT `FK_c8mn3no4e4ga6nomeju90gf87` FOREIGN KEY (`thingTypeTemplate_id`) REFERENCES `thingTypeTemplate` (`id`)
) ENGINE=InnoDB;

ALTER TABLE ThingTypeField ADD `timeToLive` bigint(20) DEFAULT NULL;
ALTER TABLE ThingTypeField ADD `typeParent` varchar(255) DEFAULT NULL;
ALTER TABLE ThingTypeField ADD `defaultValue` varchar(255) DEFAULT NULL;
ALTER TABLE ThingTypeField ADD `multiple` bit(1) DEFAULT NULL;
UPDATE ThingTypeField set multiple = false where multiple is null;

ALTER TABLE ThingTypeField ADD `thingTypeFieldTemplateId` bigint(20) DEFAULT NULL;
ALTER TABLE ThingTypeField MODIFY COLUMN type BIGINT(20);

ALTER TABLE ThingField MODIFY COLUMN type BIGINT(20);

ALTER TABLE ThingType ADD `thingTypeTemplate_id` bigint(20) DEFAULT NULL;
ALTER TABLE ThingType ADD CONSTRAINT `FK_4kmq8ve902uuqf3pbmdspq68y` FOREIGN KEY (`thingTypeTemplate_id`) REFERENCES `thingTypeTemplate` (`id`);

CREATE TABLE `connection_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `propertiesDefinitions` longtext,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_74x87xohnkq17gh6oi0m2psxg` (`group_id`),
  CONSTRAINT `FK_74x87xohnkq17gh6oi0m2psxg` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `connection0` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `properties` longtext,
  `connectionType_id` bigint(20) NOT NULL,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_lsqypoib85rtn85jvmd7ohave` (`connectionType_id`),
  KEY `FK_t7h8nemvbjh14qewo46wfv5nm` (`group_id`),
  CONSTRAINT `FK_t7h8nemvbjh14qewo46wfv5nm` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`),
  CONSTRAINT `FK_lsqypoib85rtn85jvmd7ohave` FOREIGN KEY (`connectionType_id`) REFERENCES `connection_type` (`id`)
) ENGINE=InnoDB;

-------------------------------------------------------------------------------
ALTER TABLE reportDefinition ADD `zoneOpacity` int(11) DEFAULT NULL;
ALTER TABLE reportDefinition ADD `mapOpacity` int(11) DEFAULT NULL;

ALTER TABLE reportDefinition
 ADD COLUMN editInLineEntryForm bit(1) AFTER editInLine;
-------------------------------------------------------------------------------

ALTER TABLE edgeboxrule CHANGE COLUMN outputConfig outputConfig longtext NULL DEFAULT NULL;

ALTER TABLE `thingfield` DROP FOREIGN KEY `FK_csknwrjv8eda3ay8cgm2njm08`;

UPDATE thingfield tf INNER JOIN thingtypefield ttf ON tf.name = ttf.name SET tf.thingTypeFieldId = ttf.id WHERE tf.thingTypeFieldId = 0;