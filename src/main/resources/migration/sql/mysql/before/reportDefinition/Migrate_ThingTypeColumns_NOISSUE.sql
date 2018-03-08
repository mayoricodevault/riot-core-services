ALTER TABLE reportfilter CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE reportproperty CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE reportrule CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE apc_reportgroupby CHANGE COLUMN thingTypeIdReport thingType_id  bigint(20) ;
ALTER TABLE reportfilter CHANGE COLUMN thingTypeFieldId thingTypeField_id  bigint(20) ;

ALTER TABLE reportproperty ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE reportproperty ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE reportrule ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE reportrule ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE apc_reportGroupBy ADD thingTypeField_id bigint(20) DEFAULT NULL;
ALTER TABLE apc_reportGroupBy ADD parentThingType_id bigint(20) DEFAULT NULL;

ALTER TABLE reportfilter ADD parentThingType_id bigint(20) DEFAULT NULL;

UPDATE reportfilter SET thingType_id = null WHERE thingType_id=0;
UPDATE reportproperty SET thingType_id = null WHERE thingType_id=0;
UPDATE reportrule SET thingType_id = null WHERE thingType_id=0;
UPDATE apc_reportgroupby SET thingType_id = null WHERE thingType_id=0;
