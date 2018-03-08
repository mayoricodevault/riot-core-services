ALTER TABLE apc_thing ADD modifiedTime bigint(20) DEFAULT NULL;
ALTER TABLE ThingField ADD modifiedTime bigint(20) DEFAULT NULL;
ALTER TABLE ThingType ADD modifiedTime bigint(20) DEFAULT NULL;



 UPDATE apc_thing SET modifiedTime = ( UNIX_TIMESTAMP(NOW())*1000);
 UPDATE ThingField SET modifiedTime = ( UNIX_TIMESTAMP(NOW())*1000);
 UPDATE ThingType SET modifiedTime = ( UNIX_TIMESTAMP(NOW())*1000);
