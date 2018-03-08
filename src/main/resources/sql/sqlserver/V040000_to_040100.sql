ALTER TABLE [dbo].[attachment] ADD [operationOverFile] [varchar](255) NULL
GO

CREATE TABLE [dbo].[apc_groupresources] (
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [imageIcon] [image],
  [imageTemplateName] [varchar](255) NULL,
  [group_id] [numeric](20,0) NULL,
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO
ALTER TABLE [dbo].[edgebox] ADD  port [bigint] DEFAULT 0;
GO
ALTER TABLE [dbo].[edgebox] ADD  type [varchar](255) NULL
GO


ALTER TABLE [dbo].[edgeboxrule] ADD  sortOrder [int] 0
GO
ALTER TABLE [dbo].[edgeboxrule] ADD  honorLastDetect [bit] NOT NULL DEFAULT 'FALSE';
GO
ALTER TABLE [dbo].[edgeboxrule] ADD  executeLoop [bit] NOT NULL DEFAULT 'FALSE';
GO


CREATE INDEX IDX_token_tokenExpirationTime ON token (tokenExpirationTime)
GO

CREATE INDEX IDX_thingtype_thingTypeCode ON thingtype (thingTypeCode)
GO

CREATE INDEX IDX_thingtypefield_name ON thingtypefield (name)
GO

CREATE INDEX IDX_datatype_code ON datatype (code)
GO

CREATE INDEX IDX_group0_code ON group0 (code)
GO

CREATE UNIQUE INDEX IDX_field_name ON apc_field (name)
GO

CREATE UNIQUE INDEX IDX_user_apiKey ON user0 (apiKey)
GO

CREATE UNIQUE INDEX IDX_user_username ON user0 (username)
GO