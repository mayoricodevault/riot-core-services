
DROP TABLE [dbo].[thingtypetemplatecategory]
GO

CREATE TABLE [dbo].[thingtypetemplatecategory] (
    [id] [NUMERIC](19, 0) IDENTITY(1, 1) NOT NULL,
    [code] [varchar](100) NOT NULL,
    [name] [varchar](100) NOT NULL,
    [displayOrder] [int] NOT NULL,
    [pathIcon] [VARCHAR](100) NOT NULL
    PRIMARY KEY CLUSTERED
    (
      [id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

INSERT INTO [dbo].[thingtypetemplatecategory] ([code], [displayOrder], [name], [pathIcon]) VALUES ("CUSTOM", 0, "Custom", "");

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of category', @level0type = N'Schema',
@level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypetemplatecategory', @level2type = N'Column', @level2name = 'id';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Display order of the category', @level0type = N'Schema',
@level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypetemplatecategory', @level2type = N'Column', @level2name = 'id';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The name of the category', @level0type = N'Schema',
@level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypetemplatecategory', @level2type = N'Column', @level2name = 'id';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The name icon style', @level0type = N'Schema',
@level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypetemplatecategory', @level2type = N'Column', @level2name = 'id';

ALTER TABLE [dbo].[thingtypetemplate] ADD [code] [varchar](100);
ALTER TABLE [dbo].[thingtypetemplate] ADD [displayOrder] [int](11);
ALTER TABLE [dbo].[thingtypetemplate] ADD [thingTypeTemplateCategory_id] [numeric](20);

UPDATE [dbo].[thingtypetemplate] set [code]=name, [displayOrder]=0, [thingTypeTemplateCategory_id]=1;

ALTER TABLE [dbo].[thingtypetemplate] WITH CHECK ADD CONSTRAINT [FK_thingtypetemplate_category]
   FOREIGN KEY ([thingTypeTemplateCategory_id]) REFERENCES [dbo].[thingtypetemplatecategory]([id]);

