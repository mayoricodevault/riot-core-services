

ALTER TABLE `datatype`
CHANGE `entity` `typeParent` varchar(255) DEFAULT NULL;


ALTER TABLE `thingtypefieldtemplate`
ADD `dataTypeThingTypeId` bigint(20) DEFAULT NULL;

ALTER TABLE `thingtypefieldtemplate`
CHANGE `type` `type_id` bigint(20) NOT NULL;


ALTER TABLE `thingtypefield`
ADD `dataTypeThingTypeId` bigint(20) DEFAULT NULL;

ALTER TABLE `thingtypefield`
CHANGE `type` `dataType_id` bigint(20) NOT NULL;


ALTER TABLE `reportEntryOption`
ADD `group_id` bigint(20) DEFAULT NULL;



UPDATE thingtypefieldtemplate a SET a.type_id = 27 , a.dataTypeThingTypeId = 3 WHERE id = 14;



