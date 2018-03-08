DELETE FROM `riot_main`.`datatype` WHERE `id` = 28;
INSERT INTO `riot_main`.`datatype` (`id`,`code`,`description`,`type`,`typeParent`,`value`) VALUES(28, 'ATTACHMENT', 'Attach one or many files', 'Standard Data Types', 'DATA_TYPE', 'Attachments');

/* Create table */
CREATE TABLE `attachment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment` varchar(255) DEFAULT NULL,
  `dateUploaded` datetime DEFAULT NULL,
  `fileAttachment` longblob,
  `name` varchar(255) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `uploadedBy_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_n5og1av7qw3et7ert4gqctdhk` (`uploadedBy_id`),
  CONSTRAINT `FK_n5og1av7qw3et7ert4gqctdhk` FOREIGN KEY (`uploadedBy_id`) REFERENCES `user0` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

