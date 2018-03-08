CREATE TABLE `license` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `installTime` datetime DEFAULT NULL,
  `licenseString` longtext,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_mestjo4nxmg6surk9f8lcr9tb` (`group_id`),
  CONSTRAINT `FK_mestjo4nxmg6surk9f8lcr9tb` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`)
) ENGINE=InnoDB;