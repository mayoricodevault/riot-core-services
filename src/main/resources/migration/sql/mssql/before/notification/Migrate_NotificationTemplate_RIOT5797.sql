CREATE TABLE [dbo].[notification_template](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[templateBody] [varchar](8000) NOT NULL,
	[templateName] [varchar](255) NULL,
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO