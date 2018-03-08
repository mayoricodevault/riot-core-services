ALTER TABLE [dbo].[apc_thing] ADD [modifiedTime] numeric(19,0) NULL
GO
ALTER TABLE [dbo].[ThingField] ADD [modifiedTime] numeric(19,0) NULL
GO
ALTER TABLE [dbo].[ThingType] ADD [modifiedTime] numeric(19,0) NULL
GO


CREATE FUNCTION dbo.DTtoUnixTS
(
    @dt DATETIME
)
RETURNS BIGINT
AS
BEGIN
    DECLARE @diff BIGINT
    IF @dt >= '20380119'
    BEGIN
        SET @diff = CONVERT(BIGINT, DATEDIFF(S, '19700101', '20380119'))
            + CONVERT(BIGINT, DATEDIFF(S, '20380119', @dt))
    END
    ELSE
        SET @diff = DATEDIFF(S, '19700101', @dt)
    RETURN @diff
END
GO


UPDATE apc_thing SET modifiedTime = (SELECT riot_main.dbo.DTtoUnixTS(GETDATE()) *1000)
GO
UPDATE ThingField SET modifiedTime = (SELECT riot_main.dbo.DTtoUnixTS(GETDATE()) *1000)
GO
UPDATE ThingType SET modifiedTime = (SELECT riot_main.dbo.DTtoUnixTS(GETDATE()) *1000)
GO

DROP FUNCTION dbo.DTtoUnixTS
GO
