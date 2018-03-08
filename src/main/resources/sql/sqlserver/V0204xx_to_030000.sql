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

ALTER TABLE [dbo].[license]  WITH CHECK ADD  CONSTRAINT [FK_mestjo4nxmg6surk9f8lcr9tb] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[license] CHECK CONSTRAINT [FK_mestjo4nxmg6surk9f8lcr9tb]
GO

ALTER TABLE [dbo].[reportDefinition]
ADD
[verticalTotal] [bit] NULL,
[horizontalTotal] [bit] NULL,
[timeoutCache] [int] NULL,
[zoneOpacity] [int] NULL,
[mapOpacity] [int] NULL
GO

UPDATE [dbo].[reportdefinition]
SET horizontalTotal = 1
	,verticalTotal = 1
WHERE reporttype = 'tableSummary'
			AND horizontalTotal IS NULL
			AND verticalTotal IS NULL
GO


CREATE TABLE [dbo].[reportEntryOption](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[RFIDPrint] [bit] NULL,
	[associate] [bit] NULL,
	[defaultRFIDPrint] [numeric](19, 0) NULL,
	[defaultZPLTemplate] [numeric](19, 0) NULL,
	[deleteOption] [bit] NULL,
	[disassociate] [bit] NULL,
	[displayOrder] [float] NULL,
	[editOption] [bit] NULL,
	[label] [varchar](255) NULL,
	[name] [varchar](255) NULL,
	[newOption] [bit] NULL,
	[reportDefinition_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[reportEntryOption]  WITH CHECK ADD  CONSTRAINT [FK_s6rdxd0fj3sjpqov7ikuh3vwa] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportDefinition] ([id])
GO

ALTER TABLE [dbo].[reportEntryOption] CHECK CONSTRAINT [FK_s6rdxd0fj3sjpqov7ikuh3vwa]
GO



CREATE TABLE [dbo].[reportEntryOptionProperty](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[displayOrder] [float] NULL,
	[editInline] [bit] NULL,
	[label] [varchar](255) NULL,
	[pickList] [bit] NULL,
	[propertyName] [varchar](255) NULL,
	[required] [bit] NULL,
	[sortBy] [varchar](255) NULL,
	[thingTypeFieldId] [numeric](19, 0) NULL,
	[thingTypeIdReport] [numeric](19, 0) NULL,
	[reportEntryOption_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[reportEntryOptionProperty]  WITH CHECK ADD  CONSTRAINT [FK_lilv604yhupnscoy76kmyo7ec] FOREIGN KEY([reportEntryOption_id])
REFERENCES [dbo].[reportEntryOption] ([id])
GO

ALTER TABLE [dbo].[reportEntryOptionProperty] CHECK CONSTRAINT [FK_lilv604yhupnscoy76kmyo7ec]
GO



CREATE TABLE [dbo].[entryFormPropertyData](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[name] [varchar](255) NULL,
	[value] [varchar](255) NULL,
	[reportEntryOptionProperty_id] [numeric](19, 0) NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[entryFormPropertyData]  WITH CHECK ADD  CONSTRAINT [FK_3sguvgyv2b1g36xdyfl71w3tj] FOREIGN KEY([reportEntryOptionProperty_id])
REFERENCES [dbo].[reportEntryOptionProperty] ([id])
GO

ALTER TABLE [dbo].[entryFormPropertyData] CHECK CONSTRAINT [FK_3sguvgyv2b1g36xdyfl71w3tj]
GO


CREATE TABLE [dbo].[dataType](
	[id] [numeric](19, 0) NOT NULL,
	[code] [varchar](255) NULL,
	[description] [varchar](255) NULL,
	[entity] [varchar](255) NULL,
	[type] [varchar](255) NULL,
	[value] [varchar](255) NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO


CREATE TABLE [dbo].[thingTypeTemplate](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[description] [varchar](255) NULL,
	[name] [varchar](255) NULL,
	[pathIcon] [varchar](255) NULL,
	[group_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[thingTypeTemplate]  WITH CHECK ADD  CONSTRAINT [FK_91dv2dqjcbe88rmie5akb7651] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[thingTypeTemplate] CHECK CONSTRAINT [FK_91dv2dqjcbe88rmie5akb7651]
GO


CREATE TABLE [dbo].[thingTypeFieldTemplate](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[defaultValue] [varchar](255) NULL,
	[description] [varchar](255) NULL,
	[name] [varchar](255) NULL,
	[symbol] [varchar](255) NULL,
	[type] [numeric](19, 0) NULL,
	[typeParent] [varchar](255) NULL,
	[unit] [varchar](255) NULL,
	[thingTypeTemplate_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[thingTypeFieldTemplate]  WITH CHECK ADD  CONSTRAINT [FK_c8mn3no4e4ga6nomeju90gf87] FOREIGN KEY([thingTypeTemplate_id])
REFERENCES [dbo].[thingTypeTemplate] ([id])
GO

ALTER TABLE [dbo].[thingTypeFieldTemplate] CHECK CONSTRAINT [FK_c8mn3no4e4ga6nomeju90gf87]
GO



ALTER TABLE [dbo].[ThingTypeField] ADD [timeToLive] [numeric](19, 0) NULL
GO

ALTER TABLE [dbo].[ThingTypeField] ADD [typeParent] [varchar](255) NULL
GO

ALTER TABLE [dbo].[ThingTypeField] ADD [defaultValue] [varchar](255) NULL
GO

ALTER TABLE [dbo].[ThingTypeField] ADD [multiple] [bit] NULL
GO

UPDATE ThingTypeField set multiple = 'false' where multiple is null
GO

ALTER TABLE [dbo].[ThingTypeField] ADD [thingTypeFieldTemplateId] [numeric](19, 0) NULL
GO

ALTER TABLE [dbo].[ThingTypeField] ALTER COLUMN [type] [numeric](19, 0)
GO

ALTER TABLE [dbo].[thingfield] ALTER COLUMN [type] [numeric](19, 0);
GO

ALTER TABLE [dbo].[ThingType] ADD [thingTypeTemplate_id] [numeric](19, 0) NULL
GO

ALTER TABLE [dbo].[ThingType]  WITH CHECK ADD  CONSTRAINT [FK_4kmq8ve902uuqf3pbmdspq68y] FOREIGN KEY([thingTypeTemplate_id])
REFERENCES [dbo].[thingTypeTemplate] ([id])
GO


CREATE TABLE [dbo].[connection_type](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[name] [varchar](255) NULL,
	[propertiesDefinitions] [varchar](8000) NULL,
	[group_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[connection_type]  WITH CHECK ADD  CONSTRAINT [FK_74x87xohnkq17gh6oi0m2psxg] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[connection_type] CHECK CONSTRAINT [FK_74x87xohnkq17gh6oi0m2psxg]
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
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[connection0]  WITH CHECK ADD  CONSTRAINT [FK_nusie9fp91rs5u36mjlmv0ywa] FOREIGN KEY([connectionType_id])
REFERENCES [dbo].[connection_type] ([id])
GO

ALTER TABLE [dbo].[connection0] CHECK CONSTRAINT [FK_nusie9fp91rs5u36mjlmv0ywa]
GO

ALTER TABLE [dbo].[connection0]  WITH CHECK ADD  CONSTRAINT [FK_pfs2lda4nd6b282ikxfmj1glh] FOREIGN KEY([group_id])
REFERENCES [dbo].[Group0] ([id])
GO

ALTER TABLE [dbo].[connection0] CHECK CONSTRAINT [FK_pfs2lda4nd6b282ikxfmj1glh]
GO

ALTER TABLE [dbo].[reportDefinition] ADD [editInLineEntryForm] [bit] NULL
GO

ALTER TABLE  [dbo].[edgeboxRule] ALTER COLUMN  outputConfig VARCHAR(8000) NULL
GO

ALTER TABLE [dbo].[apc_thing] ALTER COLUMN  [modifiedTime] numeric(19,0) NULL GO
ALTER TABLE [dbo].[ThingField] ALTER COLUMN  [modifiedTime] numeric(19,0) NULL GO
ALTER TABLE [dbo].[ThingType] ALTER COLUMN  [modifiedTime] numeric(19,0) NULL GO

ALTER TABLE [dbo].[ThingField] DROP CONSTRAINT [FK_csknwrjv8eda3ay8cgm2njm08] GO

UPDATE tf
SET [tf].[thingTypeFieldId] = [ttf].[id]
FROM [dbo].[thingfield] tf INNER JOIN [dbo].[thingtypefield] ttf ON [tf].[name] = [ttf].[name]
WHERE [tf].[thingTypeFieldId] = 0 GO