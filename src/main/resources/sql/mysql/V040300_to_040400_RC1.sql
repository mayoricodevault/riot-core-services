CREATE TABLE IF NOT EXISTS  `reportbulkprocess` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `affectedRecords` bigint(20) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `iniDate` datetime DEFAULT NULL,
  `omittedRecords` bigint(20) DEFAULT NULL,
  `processTime` bigint(20) DEFAULT NULL,
  `query` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `totalRecords` bigint(20) DEFAULT NULL,
  `valuesToChange` varchar(255) DEFAULT NULL,
  `createdByUser_id` bigint(20) NOT NULL,
  `reportDefinition_id` bigint(20) DEFAULT NULL,
  `thingType_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_reportBulkProcess_User0` (`createdByUser_id`),
  KEY `FK_reportbulkProcess_repDef` (`reportDefinition_id`),
  KEY `FK_reportbulkProcess_thingType` (`thingType_id`),
  CONSTRAINT `FK_reportbulkProcess_thingType` FOREIGN KEY (`thingType_id`) REFERENCES `thingtype` (`id`),
  CONSTRAINT `FK_reportBulkProcess_User0` FOREIGN KEY (`createdByUser_id`) REFERENCES `user0` (`id`),
  CONSTRAINT `FK_reportbulkProcess_repDef` FOREIGN KEY (`reportDefinition_id`) REFERENCES `reportdefinition` (`id`)
) ;