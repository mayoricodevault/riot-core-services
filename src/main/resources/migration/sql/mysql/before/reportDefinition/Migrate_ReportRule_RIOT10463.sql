UPDATE  reportrule
   SET  propertyName = CONCAT('[',propertyName,']')
  WHERE LOWER(propertyName) = 'other'
  AND reportDefinition_id IN (
       SELECT DISTINCT reportDefinition_id
        FROM apc_reportgroupby
       WHERE other);