ALTER TABLE [dbo].[reportDefinition]
ADD
[pinStyle] [varchar](255) NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[mapUnit] [varchar](255) NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[pinDisplay] [varchar](255) NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[zoneDisplay] [varchar](255) NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[clusterDistance] [int] NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[editInline] [bit] NULL
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[pinIcons] [bit] NULL
GO

ALTER TABLE logicalReader ADD CONSTRAINT logicalReader_uniqueconstraint1 UNIQUE NONCLUSTERED (code,group_id);
ALTER TABLE logicalReader ADD CONSTRAINT logicalReader_uniqueconstraint2 UNIQUE NONCLUSTERED (name,group_id);