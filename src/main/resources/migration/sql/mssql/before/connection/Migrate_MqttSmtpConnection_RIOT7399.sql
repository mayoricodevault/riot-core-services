DELETE C0 FROM dbo.connection0 AS C0, dbo.connectiontype AS CT  WHERE C0.connectionType_id = CT.id AND ( ct.code = 'MQTTConnection' OR ct.code = 'SMTPConnection' )
GO

DELETE FROM dbo.connectiontype WHERE code = 'MQTTConnection' OR code = 'SMTPConnection'
GO

UPDATE connectiontype SET description='External DataBase Connection' WHERE code='DBConnection'
GO
