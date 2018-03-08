ALTER TABLE reportDefinition ADD `pinIcons` BOOL DEFAULT NULL;
ALTER TABLE reportDefinition ADD `pinStyle` varchar(255) DEFAULT NULL;
ALTER TABLE reportDefinition ADD `mapUnit` varchar(255) DEFAULT NULL;
ALTER TABLE reportDefinition ADD `pinDisplay` varchar(255) DEFAULT NULL;
ALTER TABLE reportDefinition ADD `zoneDisplay` varchar(255) DEFAULT NULL;
ALTER TABLE reportDefinition ADD `clusterDistance` int DEFAULT NULL;
ALTER TABLE logicalReader ADD CONSTRAINT logicalReader_uniqueconstraint1 UNIQUE (code,group_id);
ALTER TABLE logicalReader ADD CONSTRAINT logicalReader_uniqueconstraint2 UNIQUE (name,group_id);