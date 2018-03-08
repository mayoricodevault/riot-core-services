ALTER TABLE dbo.reportProperty ADD showHover bit DEFAULT 0
GO

ALTER TABLE [dbo].[reportDefinition] ADD  playbackMaxThing int NULL
GO

UPDATE [dbo].[reportDefinition] SET playbackMaxThing = 100
GO

ALTER TABLE [dbo].[dataType] ADD [clazz] [varchar](255) NULL
GO
UPDATE [dbo].[dataType] SET clazz = 'java.lang.String' WHERE id IN (1,2,3,6,8,12,13,25,26, 28)
GO
UPDATE [dbo].[dataType] SET clazz = 'java.lang.Long' WHERE id = 24
GO
UPDATE [dbo].[dataType] SET clazz = 'java.lang.Boolean' WHERE id = 5
GO
UPDATE [dbo].[dataType] SET clazz = 'java.lang.BigDecimal' WHERE id = 4
GO
UPDATE [dbo].[dataType] SET clazz = 'java.util.Date' WHERE id = 11
GO
UPDATE [dbo].[dataType] SET clazz = 'com.tierconnect.riot.iot.entities.Zone' WHERE id = 9
GO
UPDATE [dbo].[dataType] SET clazz = 'com.tierconnect.riot.iot.entities.Thing' WHERE id = 27
GO
UPDATE [dbo].[dataType] SET clazz = 'com.tierconnect.riot.iot.entities.Shift' WHERE id = 7
GO
UPDATE [dbo].[dataType] SET clazz = 'com.tierconnect.riot.iot.entities.LogicalReader' WHERE id = 23
GO
UPDATE [dbo].[dataType] SET clazz = 'com.tierconnect.riot.appcore.entities.Group' WHERE id =22
GO

UPDATE dbo.datatype SET clazz = 'java.math.BigDecimal' WHERE id IN (4, 25 )
GO

UPDATE dbo.thingtypefield SET dataType_id = 24 WHERE name IN ('lastLocateTime', 'lastDetectTime')
GO


sp_RENAME 'reportfilter.thingTypeIdReport', 'thingType_id' , 'COLUMN'
GO

sp_RENAME 'reportproperty.thingTypeIdReport', 'thingType_id' , 'COLUMN'
GO

sp_RENAME 'reportrule.thingTypeIdReport', 'thingType_id' , 'COLUMN'
GO

sp_RENAME 'apc_reportgroupby.thingTypeIdReport', 'thingType_id' , 'COLUMN'
GO

sp_RENAME 'reportfilter.thingTypeFieldId', 'thingTypeField_id' , 'COLUMN'
GO

ALTER TABLE [dbo].[reportproperty] ADD [thingTypeField_id] numeric(19,0) NULL GO
ALTER TABLE [dbo].[reportproperty] ADD [parentThingType_id] numeric(19,0) NULL GO

ALTER TABLE [dbo].[reportrule] ADD [thingTypeField_id] numeric(19,0) NULL GO
ALTER TABLE [dbo].[reportrule] ADD [parentThingType_id] numeric(19,0) NULL GO

ALTER TABLE [dbo].[apc_reportgroupby] ADD [thingTypeField_id] numeric(19,0) NULL GO
ALTER TABLE [dbo].[apc_reportgroupby] ADD [parentThingType_id] numeric(19,0) NULL GO

ALTER TABLE [dbo].[reportfilter] ADD [parentThingType_id] numeric(19,0) NULL GO

UPDATE [dbo].[reportfilter] SET thingType_id = null WHERE thingType_id=0 GO
UPDATE [dbo].[reportproperty] SET thingType_id = null WHERE thingType_id=0 GO
UPDATE [dbo].[reportrule] SET thingType_id = null WHERE thingType_id=0 GO
UPDATE [dbo].[apc_reportgroupby] SET thingType_id = null WHERE thingType_id=0 GO

ALTER TABLE [dbo].[edgeboxRule] ADD [group_id] numeric(38,0)
GO

UPDATE [dbo].[edgeboxrule] set output = 'AlienReaderGPIOSubscriber' WHERE output = 'AlienReaderGPIOSubscriber2' or output = 'AlienReaderGPIOSubscriber3'
GO
