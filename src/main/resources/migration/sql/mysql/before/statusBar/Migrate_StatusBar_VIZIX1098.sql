CREATE TABLE `backgroundprocess` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id of backgroundprocess',
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
  `progress` int(20) DEFAULT 0 COMMENT 'the percentage of progress advanced',
  `threadName` varchar(255) DEFAULT NULL COMMENT 'Thread Name of the Job',
  PRIMARY KEY (`id`),
  KEY `FK_backgroundProcess_user0` (`createdByUser_id`),
  CONSTRAINT `FK_backgroundProcess_user0` FOREIGN KEY (`createdByUser_id`) REFERENCES `user0` (`id`)
) ;

CREATE TABLE `backgroundprocessdetail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id of backgroundprocessdetail',
  `endDate` datetime DEFAULT NULL COMMENT 'the date when the sub-process was completed',
  `iniDate` datetime DEFAULT NULL COMMENT 'the date when the sub-process was initialized',
  `processTime` bigint(20) DEFAULT NULL COMMENT 'the duration time of the sub-process execution',
  `query` longtext COMMENT 'the query to be executed by the sub-process',
  `status` varchar(255) DEFAULT NULL COMMENT 'the status of the sub-process',
  `totalAffectedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records affected',
  `totalOmittedRecords` bigint(20) DEFAULT NULL COMMENT 'the total records omitted',
  `totalRecords` bigint(20) DEFAULT NULL COMMENT 'the total of records to be processed',
  `valuesToChange` varchar(255) DEFAULT NULL COMMENT 'the values to change',
  `backgroundProcess_id` bigint(20) NOT NULL COMMENT 'the id reference to the main process',
  `thingType_id` bigint(20) DEFAULT NULL COMMENT 'the id reference to the thing type',
  PRIMARY KEY (`id`),
  KEY `FK_backgroundProcess_` (`backgroundProcess_id`),
  KEY `FK_backgroundProcess_thingType` (`thingType_id`),
  CONSTRAINT `FK_backgroundProcess_thingType` FOREIGN KEY (`thingType_id`) REFERENCES `thingtype` (`id`),
  CONSTRAINT `FK_backgroundProcess_` FOREIGN KEY (`backgroundProcess_id`) REFERENCES `backgroundprocess` (`id`)
) ;

CREATE TABLE `backgroundprocessdetaillog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT  COMMENT 'ID of backgroundprocessdetaillog',
  `status` varchar(255) DEFAULT NULL COMMENT 'Status of the register: ADDED',
  `thingId` bigint(20) NOT NULL COMMENT 'Thing ID to be Deleted',
  `thingTypeCode` VARCHAR(255) NOT NULL COMMENT 'thingTypeCode of the deleted thing',
  `serialNumber` VARCHAR(255) NOT NULL COMMENT 'serialNumber of the deleted thing',
  `backgroundProcessDetail_id` bigint(20) NOT NULL COMMENT 'Background Process Detail ID reference',
  PRIMARY KEY (`id`),
  KEY `FK_backgroundProcessDetail_Log` (`backgroundProcessDetail_id`),
  CONSTRAINT `FK_backgroundProcessDetail_Log` FOREIGN KEY (`backgroundProcessDetail_id`) REFERENCES `backgroundprocessdetail` (`id`)
);

CREATE TABLE `backgroundprocessentity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT  COMMENT 'ID of backgroundprocessentity',
  `columnName` varchar(255) DEFAULT NULL COMMENT 'Name of id column used for process',
  `columnValue` VARCHAR(255) NOT NULL COMMENT 'value of process id',
  `moduleName` VARCHAR(255) NOT NULL COMMENT 'module name for running process',
  `backgroundProcess_id` bigint(20) NOT NULL COMMENT 'Background Process ID reference',
  PRIMARY KEY (`id`),
  KEY `FK_backgroundProcessEntity` (`backgroundProcess_id`),
  CONSTRAINT `FK_backgroundProcessEntity` FOREIGN KEY (`backgroundProcess_id`) REFERENCES `backgroundprocess` (`id`)
);
