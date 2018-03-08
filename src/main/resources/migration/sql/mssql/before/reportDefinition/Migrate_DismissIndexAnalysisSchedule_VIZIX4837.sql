delete from userfield WHERE field_id in (select id from riot_main.apc_field where module = 'Reports' and name = 'reportLogCron');
delete from groupfield WHERE field_id in (select id from riot_main.apc_field where module = 'Reports' and name = 'reportLogCron');
delete from apc_field WHERE module = 'Reports' and name = 'reportLogCron';
GO