CREATE TABLE IF NOT EXISTS  `thingtypepath` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID of the table',
  `originThingType_id`  bigint(20) NOT NULL  COMMENT 'Thing Type ID origin',
  `destinyThingType_id` bigint(20) NOT NULL   COMMENT 'Thing Type ID destiny',
   `path` varchar(255)  DEFAULT NULL   COMMENT 'Path to get the value from originThingTypeId to destinyThingType_id',
  PRIMARY KEY (`id`)
  );

 ALTER TABLE riot_main.thingtypepath
                ADD CONSTRAINT FK_thingtypepath_originThingType FOREIGN KEY (originThingType_id) REFERENCES thingtype (id),
                ADD CONSTRAINT  FK_thingtypepath_destinyThingType FOREIGN KEY (destinyThingType_id) REFERENCES thingtype (id);

