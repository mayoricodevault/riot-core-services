ALTER TABLE reportproperty ADD showHover bit(1) NULL DEFAULT false;

ALTER TABLE reportDefinition ADD COLUMN playbackMaxThing int(11);
UPDATE reportDefinition SET playbackMaxThing = 100 ;

ALTER TABLE dataType ADD COLUMN clazz VARCHAR(255);
UPDATE dataType SET clazz = 'java.lang.String' WHERE id IN (1,2,3,6,8,12,13,25,26,28);
UPDATE dataType SET clazz = 'java.lang.Long' WHERE id = 24;
UPDATE dataType SET clazz = 'java.lang.Boolean' WHERE id = 5;
UPDATE dataType SET clazz = 'java.lang.BigDecimal' WHERE id = 4;
UPDATE dataType SET clazz = 'java.util.Date' WHERE id = 11;
UPDATE dataType SET clazz = 'com.tierconnect.riot.iot.entities.Zone' WHERE id = 9;
UPDATE dataType SET clazz = 'com.tierconnect.riot.iot.entities.Thing' WHERE id = 27;
UPDATE dataType SET clazz = 'com.tierconnect.riot.iot.entities.Shift' WHERE id = 7;
UPDATE dataType SET clazz = 'com.tierconnect.riot.iot.entities.LogicalReader' WHERE id = 23;
UPDATE dataType SET clazz = 'com.tierconnect.riot.appcore.entities.Group' WHERE id =22;


UPDATE riot_main.datatype SET clazz = 'java.math.BigDecimal' WHERE id IN (4, 25 );

UPDATE riot_main.thingtypefield a SET a.dataType_id = 24 WHERE a.name IN ('lastLocateTime', 'lastDetectTime');


ALTER TABLE riot_main.reportfilter CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE riot_main.reportproperty CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE riot_main.reportrule CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE riot_main.apc_reportgroupby CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE riot_main.reportfilter CHANGE COLUMN thingTypeFieldId thingTypeField_id  bigint(20) ;

ALTER TABLE reportproperty ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE reportproperty ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE reportrule ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE reportrule ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE apc_reportGroupBy ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE apc_reportGroupBy ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE reportfilter ADD parentThingType_id bigint(20) DEFAULT NULL;

UPDATE riot_main.reportfilter SET thingType_id = null WHERE thingType_id=0;
UPDATE riot_main.reportproperty SET thingType_id = null WHERE thingType_id=0;
UPDATE riot_main.reportrule SET thingType_id = null WHERE thingType_id=0;
UPDATE riot_main.apc_reportgroupby SET thingType_id = null WHERE thingType_id=0;

ALTER TABLE edgeboxRule ADD COLUMN group_id bigint(255);

UPDATE edgeboxrule set output = 'AlienReaderGPIOSubscriber' WHERE output = 'AlienReaderGPIOSubscriber2' or output = 'AlienReaderGPIOSubscriber3';
