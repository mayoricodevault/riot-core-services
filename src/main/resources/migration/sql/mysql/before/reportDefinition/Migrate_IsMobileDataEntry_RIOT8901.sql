ALTER TABLE reportdefinition ADD COLUMN isMobileDataEntry bit(1) DEFAULT 0;

ALTER TABLE reportentryoption ADD COLUMN isMobile bit(1) DEFAULT 0;
