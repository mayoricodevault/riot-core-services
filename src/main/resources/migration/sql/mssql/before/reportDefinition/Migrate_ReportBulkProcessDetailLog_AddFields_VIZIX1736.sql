ALTER TABLE [dbo].[reportbulkprocessdetaillog] ADD [serialNumber] VARCHAR(255) NOT NULL
GO

ALTER TABLE [dbo].[reportbulkprocessdetaillog] ADD [thingTypeCode] VARCHAR(255) NOT NULL
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'serialNumber of the deleted thing', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'serialNumber';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'thingTypeCode of the deleted thing', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'thingTypeCode';
GO