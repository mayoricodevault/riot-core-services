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
