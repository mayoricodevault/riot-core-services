ALTER TABLE apc_reportGroupBy ADD COLUMN byPartition bit(1) DEFAULT 0;
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

ALTER TABLE reportdefinition ADD COLUMN heatmap bit(1) DEFAULT 0;
ALTER TABLE reportdefinition ADD COLUMN heatmapBlur int(11) DEFAULT NULL;
ALTER TABLE reportdefinition ADD COLUMN heatmapLabel varchar(255) DEFAULT NULL;
ALTER TABLE reportdefinition ADD COLUMN heatmapRadio int(1) DEFAULT NULL;
ALTER TABLE reportdefinition ADD COLUMN isMobileDataEntry bit(1) DEFAULT 0;

ALTER TABLE reportentryoption ADD COLUMN isMobile bit(1) DEFAULT 0;

ALTER TABLE reportproperty ADD COLUMN enableheat bit(1) DEFAULT 0;

ALTER TABLE thingtypefieldtemplate ADD COLUMN timeSeries bit(1) DEFAULT 0;

ALTER TABLE role DROP COLUMN code;

ALTER TABLE thingtypefield CHANGE COLUMN defaultValue defaultValue VARCHAR(1024) DEFAULT NULL;

ALTER TABLE thingtypefieldtemplate CHANGE COLUMN defaultValue defaultValue VARCHAR(1024) DEFAULT NULL;