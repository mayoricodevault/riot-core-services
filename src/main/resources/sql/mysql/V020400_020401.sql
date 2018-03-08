/*RIOT-4848 - Reports > Add a new attribute 'isMobile' into ReportDefinition table*/
ALTER TABLE reportdefinition ADD isMobile bit(1) DEFAULT NULL;
