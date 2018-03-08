--Remove old connections with MQTTConnection and SMTPConnection types
DELETE C0 FROM connection0 AS C0, connectiontype AS CT  WHERE C0.connectionType_id = CT.id AND ( ct.code = 'MQTTConnection' OR ct.code = 'SMTPConnection' );

--Remove MQTTConnection and SMTPConnection types
DELETE FROM connectiontype WHERE code = 'MQTTConnection' OR code = 'SMTPConnection';

--Update database connection type code
UPDATE connectiontype SET description='External DataBase Connection' WHERE code='DBConnection';
