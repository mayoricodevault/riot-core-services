CREATE INDEX IDX_token_tokenExpirationTime ON token (tokenExpirationTime);

CREATE INDEX IDX_thingtype_thingTypeCode ON thingtype (thingTypeCode);

CREATE INDEX IDX_thingtypefield_name ON thingtypefield (name);

CREATE INDEX IDX_datatype_code ON datatype (code);

CREATE INDEX IDX_group0_code ON group0 (code);

CREATE UNIQUE INDEX IDX_field_name ON apc_field (name);

CREATE UNIQUE INDEX IDX_user_apiKey ON user0 (apiKey);

CREATE UNIQUE INDEX IDX_user_username ON user0 (username);