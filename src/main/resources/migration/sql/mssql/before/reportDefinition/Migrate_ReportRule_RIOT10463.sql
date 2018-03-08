UPDATE [dbo].[reportrule] SET  propertyName = '['+ propertyName + ']'
WHERE LOWER(propertyName) = 'other'
      AND reportDefinition_id IN ( SELECT DISTINCT reportDefinition_id
                                   FROM [dbo].[apc_reportgroupby]
                                   WHERE other = 1)
GO