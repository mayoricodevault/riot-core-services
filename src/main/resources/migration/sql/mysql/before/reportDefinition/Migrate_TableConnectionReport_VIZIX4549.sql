CREATE TABLE `reportcustomfilter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `displayOrder` float DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `operator` varchar(255) DEFAULT NULL,
  `propertyName` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `editable` bit(1) DEFAULT NULL,
  `dataTypeId` bigint(20) NOT NULL,
  `reportDefinition_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_customfilter_reportdefinition` (`reportDefinition_id`),
  CONSTRAINT `FK_customfilter_reportdefinition` FOREIGN KEY (`reportDefinition_id`) REFERENCES `reportdefinition` (`id`)
);