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