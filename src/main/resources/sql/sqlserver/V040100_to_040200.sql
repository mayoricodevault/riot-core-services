UPDATE [dbo].[reportrule] SET  propertyName = '['+ propertyName + ']'
 WHERE LOWER(propertyName) = 'other'
   AND reportDefinition_id IN ( SELECT DISTINCT reportDefinition_id
                                    FROM [dbo].[apc_reportgroupby]
                                   WHERE other = 1)
GO
ALTER TABLE dbo.version ADD computerIP VARCHAR(255)
GO
ALTER TABLE dbo.version ADD computerName VARCHAR(255)
GO
ALTER TABLE dbo.version ADD computerUser VARCHAR(255)
GO
ALTER TABLE [dbo].[Resource] DROP COLUMN module
GO
