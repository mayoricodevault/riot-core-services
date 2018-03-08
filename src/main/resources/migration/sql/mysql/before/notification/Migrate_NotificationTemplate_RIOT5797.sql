CREATE TABLE `notification_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `templateBody` longtext,
  `templateName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);
