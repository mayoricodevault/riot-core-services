CREATE TABLE [dbo].parameters(
  [id] NUMERIC(20) NOT NULL IDENTITY(1,1),
  [category] [varchar](255) NULL,
  [code] [varchar](255) NULL,
  [appresourcecode] [varchar](255) NULL,
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[edgeboxRule] ADD  conditionType NVARCHAR(MAX) NULL
GO
ALTER TABLE [dbo].[edgeboxRule] ADD  parameterConditionType NVARCHAR(MAX) NULL
GO
ALTER TABLE [dbo].[edgebox] ADD  parameterType NVARCHAR(MAX) NULL
GO