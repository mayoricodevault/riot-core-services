DELIMITER $$ CREATE VIEW [thingtypedirectionmap] AS
    SELECT
        `ttp`.`id` AS `thingTypeParentId`,
        `ttp`.`thingTypeCode` AS `thingTypeParentCode`,
        `ttp`.`group_id` AS `thingTypeParentGroupId`,
        `ttc`.`id` AS `thingTypeChildId`,
        `ttc`.`thingTypeCode` AS `thingTypeChildCode`,
        `ttc`.`group_id` AS `thingTypeChildGroupId`,
        'down' AS `mapDirection`
    FROM
        ((`thingtype` `ttp`
        JOIN `thingtypemap` `ttm` ON ((`ttp`.`id` = `ttm`.`parent_id`)))
        JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttm`.`child_id`)))
    UNION SELECT
        `ttp`.`id` AS `thingTypeParentId`,
        `ttp`.`thingTypeCode` AS `thingTypeParentCode`,
        `ttp`.`group_id` AS `thingTypeParentGroupId`,
        `ttc`.`id` AS `thingTypeChildId`,
        `ttc`.`thingTypeCode` AS `thingTypeChildCode`,
        `ttc`.`group_id` AS `thingTypeChildGroupId`,
        'down' AS `mapDirection`
    FROM
        ((`thingtype` `ttp`
        JOIN `thingtypefield` `ttcf` ON ((`ttp`.`id` = `ttcf`.`dataTypeThingTypeId`)))
        JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttcf`.`thingType_id`)))
    WHERE
        (`ttp`.`isParent` = TRUE AND ttcf.typeParent = 'NATIVE_THING_TYPE')
    UNION SELECT
         `ttp`.`id` AS `thingTypeParentId`,
        `ttp`.`thingTypeCode` AS `thingTypeParentCode`,
        `ttp`.`group_id` AS `thingTypeParentGroupId`,
        `ttc`.`id` AS `thingTypeChildId`,
        `ttc`.`thingTypeCode` AS `thingTypeChildCode`,
        `ttc`.`group_id` AS `thingTypeChildGroupId`,
        'down' AS `mapDirection`
    FROM
        ((`thingtype` `ttp`
        JOIN `thingtypefield` `ttpf` ON ((`ttp`.`id` = `ttpf`.`thingType_id`)))
        JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttpf`.`dataTypeThingTypeId`)))
    WHERE
        (`ttc`.`isParent` = FALSE  AND ttpf.typeParent = 'NATIVE_THING_TYPE');
$$ DELIMITER;