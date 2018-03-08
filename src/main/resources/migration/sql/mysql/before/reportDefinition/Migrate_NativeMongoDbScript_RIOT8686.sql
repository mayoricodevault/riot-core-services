ALTER TABLE reportdefinition ADD COLUMN chartViewProperties0 longtext DEFAULT NULL;

CREATE TABLE `reportdefinitionconfig` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `keyType` varchar(255) DEFAULT NULL,
  `keyValue` varchar(8000) DEFAULT NULL,
  `reportDefinition_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_qoum8q7l69497ojn6ccxuj7u5` (`reportDefinition_id`),
  CONSTRAINT `FK_qoum8q7l69497ojn6ccxuj7u5` FOREIGN KEY (`reportDefinition_id`) REFERENCES `reportdefinition` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=latin1;
