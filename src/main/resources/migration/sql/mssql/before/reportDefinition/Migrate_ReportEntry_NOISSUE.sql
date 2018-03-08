UPDATE [dbo].[reportentryoption]
   SET group_id = (SELECT group_id
                      FROM[dbo].[reportdefinition]
							WHERE id = reportDefinition_id)
GO
