DROP TABLE `connection0`;

DROP TABLE `connection_type`;

CREATE TABLE `connectionType` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `propertiesDefinitions` longtext,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_2wm92f8txnvj6s3r6kirx2eux` (`code`),
  KEY `FK_c345gl1cmg9jaak4hhlgobnq7` (`group_id`),
  CONSTRAINT `FK_c345gl1cmg9jaak4hhlgobnq7` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;


CREATE TABLE `connection0` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `properties` longtext,
  `connectionType_id` bigint(20) NOT NULL,
  `group_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_3n8yg35ryw9qbsgheytb6nag9` (`name`),
  KEY `FK_nusie9fp91rs5u36mjlmv0ywa` (`connectionType_id`),
  KEY `FK_pfs2lda4nd6b282ikxfmj1glh` (`group_id`),
  CONSTRAINT `FK_pfs2lda4nd6b282ikxfmj1glh` FOREIGN KEY (`group_id`) REFERENCES `Group0` (`id`),
  CONSTRAINT `FK_nusie9fp91rs5u36mjlmv0ywa` FOREIGN KEY (`connectionType_id`) REFERENCES `connectionType` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;