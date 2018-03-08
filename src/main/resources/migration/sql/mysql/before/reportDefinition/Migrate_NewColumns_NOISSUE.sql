ALTER TABLE reportdefinition ADD COLUMN heatmap bit(1) DEFAULT 0;
ALTER TABLE reportdefinition ADD COLUMN heatmapBlur int(11) DEFAULT NULL;
ALTER TABLE reportdefinition ADD COLUMN heatmapLabel varchar(255) DEFAULT NULL;
ALTER TABLE reportdefinition ADD COLUMN heatmapRadio int(1) DEFAULT NULL;
