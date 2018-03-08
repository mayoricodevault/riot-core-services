CREATE TABLE [dbo].[license](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[installTime] [datetime] NULL,
	[licenseString] [varchar](8000) NULL,
	[group_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO