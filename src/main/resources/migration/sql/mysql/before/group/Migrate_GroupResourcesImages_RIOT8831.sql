CREATE TABLE IF NOT EXISTS `apc_groupresources` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `imageIcon` longblob,
  `imageTemplateName` varchar(255) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_mbvgldapp14qytn24otlht001` (`group_id`),
  CONSTRAINT `FK_mbvgldapp14qytn24otlht001` FOREIGN KEY (`group_id`) REFERENCES `group0` (`id`)
);