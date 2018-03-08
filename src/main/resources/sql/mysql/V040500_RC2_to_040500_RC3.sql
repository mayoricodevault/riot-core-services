CREATE TABLE IF NOT EXISTS `riot_main`.`importexport` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `duration` bigint(20) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `errorRecord` bigint(20) DEFAULT NULL,
  `processType` varchar(255) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `successRecord` bigint(20),
  `totalRecord` bigint(20),
  `userId` bigint(20) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB;
