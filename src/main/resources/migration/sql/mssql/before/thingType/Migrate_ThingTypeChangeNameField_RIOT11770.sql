DROP INDEX [dbo].[ThingTypeField].[IDX_thingtypefield_name]
GO

DECLARE @collation AS varchar(1000)
SET @collation = (SELECT replace(collation_name, 'CI', 'CS')
                  FROM sys.columns
                  where name='name'
                        and object_id=(SELECT object_id
                                       FROM sys.tables
                                       where name='ThingTypeField'))
IF @collation IS NULL
    SET @collation = 'SQL_Latin1_General_CP1_CS_AS'
EXEC ('ALTER TABLE [dbo].[ThingTypeField] ALTER COLUMN [name] [varchar](255) COLLATE ' + @collation)
GO

CREATE INDEX IDX_thingtypefield_name ON [dbo].[ThingTypeField](name ASC)
GO
