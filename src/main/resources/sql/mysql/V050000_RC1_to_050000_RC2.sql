DROP TABLE `reportbulkprocess`;

CREATE TABLE `reportbulkprocess` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id of reportbulkprocess',
  `checked` bit(1) NOT NULL DEFAULT 0 COMMENT 'determines if the bulk process message on UI was close by user',
  `endDate` datetime DEFAULT NULL COMMENT 'the date when the process was complete',
  `iniDate` datetime DEFAULT NULL COMMENT 'the date when the process was initialize',
  `processTime` bigint(20) DEFAULT NULL COMMENT 'the duration time of the process execution',
  `status` varchar(255) DEFAULT NULL COMMENT 'the status of the process',
  `totalAffectedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records affected',
  `totalOmittedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records omitted',
  `totalRecords` bigint(20) DEFAULT NULL COMMENT 'the total of records to be processed',
  `typeProcess` varchar(255) DEFAULT NULL COMMENT 'the type of process to execute (update/delete)',
  `createdByUser_id` bigint(20) NOT NULL COMMENT 'the id of the user that executed the bulk process',
  `reportDefinition_id` bigint(20) DEFAULT NULL COMMENT 'the id of the report over the bulk process was executed',
  `progress` int(20) DEFAULT 0 COMMENT 'the percentage of progress advanced',
  `threadName` varchar(255) DEFAULT NULL COMMENT 'Thread Name of the Job',
  PRIMARY KEY (`id`),
  KEY `FK_reportBulkProcess_user0` (`createdByUser_id`),
  KEY `FK_reportbulkProcess_repDef` (`reportDefinition_id`),
  CONSTRAINT `FK_reportbulkProcess_repDef` FOREIGN KEY (`reportDefinition_id`) REFERENCES `reportdefinition` (`id`),
  CONSTRAINT `FK_reportBulkProcess_user0` FOREIGN KEY (`createdByUser_id`) REFERENCES `user0` (`id`)
) ;

CREATE TABLE `reportbulkprocessdetail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id of reportbulkprocessdetail',
  `endDate` datetime DEFAULT NULL COMMENT 'the date when the sub-process was completed',
  `iniDate` datetime DEFAULT NULL COMMENT 'the date when the sub-process was initialized',
  `processTime` bigint(20) DEFAULT NULL COMMENT 'the duration time of the sub-process execution',
  `query` longtext COMMENT 'the query to be executed by the sub-process',
  `status` varchar(255) DEFAULT NULL COMMENT 'the status of the sub-process',
  `totalAffectedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records affected',
  `totalOmittedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records omitted',
  `totalRecords` bigint(20) DEFAULT NULL COMMENT 'the total of records to be processed',
  `valuesToChange` varchar(255) DEFAULT NULL COMMENT 'the values to change',
  `reportBulkProcess_id` bigint(20) NOT NULL COMMENT 'the id reference to the main process',
  `thingType_id` bigint(20) DEFAULT NULL COMMENT 'the id reference to the thing type',
  PRIMARY KEY (`id`),
  KEY `FK_reportbulkProcess_` (`reportBulkProcess_id`),
  KEY `FK_reportbulkProcess_thingType` (`thingType_id`),
  CONSTRAINT `FK_reportbulkProcess_thingType` FOREIGN KEY (`thingType_id`) REFERENCES `thingtype` (`id`),
  CONSTRAINT `FK_reportbulkProcess_` FOREIGN KEY (`reportBulkProcess_id`) REFERENCES `reportbulkprocess` (`id`)
) ;

CREATE TABLE `reportbulkprocessdetaillog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT  COMMENT 'ID of reportbulkprocessdetaillog',
  `status` varchar(255) DEFAULT NULL COMMENT 'Status of the register: ADDED',
  `thingId` bigint(20) NOT NULL COMMENT 'Thing ID to be Deleted',
  `reportBulkProcessDetail_id` bigint(20) NOT NULL COMMENT 'Report Bulk Process Detail ID reference',
  PRIMARY KEY (`id`),
  KEY `FK_reportBulkProcessDetail_Log` (`reportBulkProcessDetail_id`),
  CONSTRAINT `FK_reportBulkProcessDetail_Log` FOREIGN KEY (`reportBulkProcessDetail_id`) REFERENCES `reportbulkprocessdetail` (`id`)
);



