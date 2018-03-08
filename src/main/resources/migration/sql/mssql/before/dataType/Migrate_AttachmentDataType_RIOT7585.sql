DELETE FROM [dbo].[datatype]  WHERE [id] = 28 GO
INSERT INTO [dbo].[datatype] ([id],[code],[description],[type],[typeParent],[value]) VALUES(28, 'ATTACHMENT', 'Attach one or many files', 'Standard Data Types', 'DATA_TYPE', 'Attachments') GO

CREATE TABLE [dbo].[attachment](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[comment] [varchar](255) NULL,
	[dateUploaded] [datetime] NULL,
	[fileAttachment] [image] NULL,
	[name] [varchar](255) NULL,
	[size] [numeric](19, 0) NULL,
	[type] [varchar](255) NULL,
	[uploadedBy_id] [numeric](19, 0) NOT NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO


