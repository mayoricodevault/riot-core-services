UPDATE [dbo].[datatype] SET value = 'Expression', description = 'Expression of formula' WHERE id=26
GO

ALTER TABLE [dbo].[reportEntryOption] ALTER COLUMN [group_id] [numeric](19, 0)  NULL
GO


