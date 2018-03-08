DROP TABLE [dbo].[connection0]
GO

DROP TABLE [dbo].[connectionType]
GO


CREATE TABLE [dbo].[connectionType](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[code] [varchar](255) NULL,
	[description] [varchar](255) NULL,
	[propertiesDefinitions] [varchar](8000) NULL,
	[group_id] [numeric](19, 0) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UK_2wm92f8txnvj6s3r6kirx2eux] UNIQUE NONCLUSTERED
(
	[code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

ALTER TABLE [dbo].[connectionType]  WITH CHECK ADD  CONSTRAINT [FK_c345gl1cmg9jaak4hhlgobnq7] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[connectionType] CHECK CONSTRAINT [FK_c345gl1cmg9jaak4hhlgobnq7]
GO

CREATE TABLE [dbo].[connection0](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[name] [varchar](255) NULL,
	[properties] [varchar](8000) NULL,
	[connectionType_id] [numeric](19, 0) NOT NULL,
	[group_id] [numeric](19, 0) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UK_3n8yg35ryw9qbsgheytb6nag9] UNIQUE NONCLUSTERED
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

ALTER TABLE [dbo].[connection0]  WITH CHECK ADD  CONSTRAINT [FK_nusie9fp91rs5u36mjlmv0ywa] FOREIGN KEY([connectionType_id])
REFERENCES [dbo].[connectionType] ([id])
GO

ALTER TABLE [dbo].[connection0] CHECK CONSTRAINT [FK_nusie9fp91rs5u36mjlmv0ywa]
GO

ALTER TABLE [dbo].[connection0]  WITH CHECK ADD  CONSTRAINT [FK_pfs2lda4nd6b282ikxfmj1glh] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[connection0] CHECK CONSTRAINT [FK_pfs2lda4nd6b282ikxfmj1glh]
GO

CREATE TABLE [dbo].[sequence](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[currentValue] [numeric](19, 0) NULL,
	[initialValue] [numeric](19, 0) NULL,
	[name] [varchar](255) NULL,
	[thingTypeField_id] [numeric](19, 0) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_PADDING OFF
GO

ALTER TABLE [dbo].[sequence]  WITH CHECK ADD  CONSTRAINT [FK_kdtiey768mmsvfdwaiy7ktasl] FOREIGN KEY([thingTypeField_id])
REFERENCES [dbo].[ThingTypeField] ([id])
GO

ALTER TABLE [dbo].[sequence] CHECK CONSTRAINT [FK_kdtiey768mmsvfdwaiy7ktasl]
GO


ALTER TABLE [dbo].[group0] ADD [hierarchyName] varchar(512) GO

ALTER TABLE [dbo].[shift] ADD  [code] VARCHAR(255) GO

ALTER TABLE [dbo].[reportDefinition] ADD  delete0 bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[reportDefinition] ADD  dismiss bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[reportDefinition] ADD  rfidPrint bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[thingtype] ADD serialFormula VARCHAR(255) ;