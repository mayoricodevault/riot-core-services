ALTER TABLE [dbo].[user0] ADD [timezone] varchar(255)
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Time Zone Configuration', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'user0', @level2type = N'Column', @level2name = 'timezone';
GO

ALTER TABLE [dbo].[user0] ADD [dateFormat] varchar(255)
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Date Format Configuration', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'user0', @level2type = N'Column', @level2name = 'dateFormat';
GO

ALTER TABLE [dbo].[reportdefinition] ADD [dateFormatColumns] BIT DEFAULT 0
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Include date format columns in export', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportdefinition', @level2type = N'Column', @level2name = 'dateFormatColumns';
GO