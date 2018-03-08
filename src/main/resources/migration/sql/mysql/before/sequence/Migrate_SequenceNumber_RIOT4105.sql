CREATE TABLE `sequence` (
  `id` bigint(20) NOT NULL auto_increment,
  `currentValue` bigint(20) default NULL,
  `initialValue` bigint(20) default NULL,
  `name` varchar(255) default NULL,
  `thingTypeField_id` bigint(20) default NULL,
  PRIMARY KEY  (`id`),
  KEY `FK_kdtiey768mmsvfdwaiy7ktasl` (`thingTypeField_id`),
  CONSTRAINT `FK_kdtiey768mmsvfdwaiy7ktasl` FOREIGN KEY (`thingTypeField_id`) REFERENCES `thingtypefield` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

ALTER TABLE shift ADD COLUMN code VARCHAR(255);

ALTER TABLE reportDefinition ADD COLUMN `delete0` bit(1) NOT NULL DEFAULT FALSE;

ALTER TABLE reportDefinition ADD COLUMN `dismiss` bit(1) NOT NULL DEFAULT FALSE;

ALTER TABLE reportDefinition ADD COLUMN `rfidPrint` bit(1) NOT NULL DEFAULT FALSE ;

ALTER TABLE thingtype ADD COLUMN serialFormula VARCHAR(255) ;