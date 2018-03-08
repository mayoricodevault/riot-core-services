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