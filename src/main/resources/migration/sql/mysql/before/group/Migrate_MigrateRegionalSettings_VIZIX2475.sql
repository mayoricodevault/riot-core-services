ALTER TABLE user0 ADD COLUMN timezone VARCHAR(255)  COMMENT 'Time Zone Configuration';
ALTER TABLE user0 ADD COLUMN dateFormat VARCHAR(255)  COMMENT 'Date Format Configuration';
ALTER TABLE reportdefinition ADD COLUMN dateFormatColumns BIT(1) NOT NULL DEFAULT 0 COMMENT 'Include date format columns in export';