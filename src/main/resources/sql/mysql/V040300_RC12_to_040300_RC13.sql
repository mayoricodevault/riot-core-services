CREATE TABLE IF NOT EXISTS parameters
(
    id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    appResourceCode VARCHAR(255),
    category VARCHAR(255),
    code VARCHAR(255),
    value VARCHAR(8000)
);
ALTER TABLE edgeboxRule ADD COLUMN conditionType varchar(255) DEFAULT NULL;
ALTER TABLE edgeboxRule ADD COLUMN parameterConditionType varchar(255) DEFAULT NULL;

ALTER TABLE edgebox ADD COLUMN parameterType varchar(255) DEFAULT NULL;

ALTER TABLE apc_field ADD COLUMN `parentField_id` bigint(20) DEFAULT NULL;

