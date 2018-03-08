
ALTER TABLE [dbo].[thingtypefieldtemplate] ADD [dataTypeThingTypeId] [numeric](19, 0)
GO


ALTER TABLE [dbo].[thingtypefield] ADD [dataTypeThingTypeId] [numeric](19, 0)
GO

ALTER TABLE [dbo].[reportEntryOption] ADD [group_id] [numeric](19, 0)
GO

ALTER TABLE [dbo].[reportEntryOption] ADD CONSTRAINT FK_reportEntryOption_group FOREIGN KEY(group_id)
	REFERENCES [dbo].[group0]  (id)
GO

UPDATE [dbo].[thingtypefieldtemplate] SET type = 27 , dataTypeThingTypeId = 3 WHERE id = 14
GO

EXEC sp_rename 'dataType.entity', 'typeParent'
GO
EXEC sp_rename 'thingtypefieldtemplate.type', 'type_id'
GO
EXEC sp_rename 'thingtypefield.type', 'dataType_id'
GO
