UPDATE  reportrule
   SET  propertyName = CONCAT('[',propertyName,']')
  WHERE LOWER(propertyName) = 'other'
  AND reportDefinition_id IN (
       SELECT DISTINCT reportDefinition_id
        FROM apc_reportgroupby
       WHERE other);
ALTER TABLE version ADD computerIP varchar(255) NULL;
ALTER TABLE version ADD computerName varchar(255) NULL;
ALTER TABLE version ADD computerUser varchar(255) NULL;
ALTER TABLE Resource DROP COLUMN module;
UPDATE edgeboxrule SET outputconfig='{}' WHERE output='ReportGeneratorSubscriber';
