ALTER TABLE `riot_main`.`localMap` ADD `rotationDegree` DOUBLE;

CREATE TABLE IF NOT EXISTS `localmappoint` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `arrayIndex` bigint(20) DEFAULT NULL,
  `x` double DEFAULT NULL,
  `y` double DEFAULT NULL,
  `localMap_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_qk4vcf03cqdbk2bpineusepyl` (`localMap_id`),
  CONSTRAINT `FK_qk4vcf03cqdbk2bpineusepyl` FOREIGN KEY (`localMap_id`) REFERENCES `localmap` (`id`)
);