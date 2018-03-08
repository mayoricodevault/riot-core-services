ALTER TABLE
    `reportbulkprocessdetaillog` ADD (`serialNumber` VARCHAR(255) NOT NULL COMMENT 'serialNumber of the deleted thing') ;

ALTER TABLE
    `reportbulkprocessdetaillog` ADD (`thingTypeCode` VARCHAR(255) NOT NULL COMMENT 'thingTypeCode of the deleted thing');