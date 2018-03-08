
CREATE TABLE field_value_history (
  field_id bigint,
  at timestamp,
  value text,
  x float,
  y float,
  PRIMARY KEY(field_id,at)
)  WITH gc_grace_seconds = 1;

CREATE INDEX ON field_value_history(x);
CREATE INDEX ON field_value_history(y);

CREATE TABLE field_value_history2 (
  field_type_id bigint,             //field type id
  thing_id bigint,                  //thing id
  time timestamp,
  value text,
  PRIMARY KEY((thing_id,field_type_id),time)
)  WITH gc_grace_seconds = 1;

/** hold the last value for thing **/
CREATE TABLE field_value (
  field_id bigint,                  //field id
  thing_id bigint,                  //thing id
  value    text,                    //value
  time     timestamp,               //field_id value
  PRIMARY KEY(field_id)
);

CREATE INDEX ON field_value( thing_id );
CREATE INDEX ON field_value( value );

/** hold the last value for thing **/
CREATE TABLE field_type (
  field_type_id bigint,             //field type id
  thing_id bigint,                  //thing id
  value    text,                    //value
  time     timestamp,               //field_id value
  PRIMARY KEY(field_type_id, thing_id)
);

CREATE INDEX ON field_type( thing_id );
CREATE INDEX ON field_type( value );




/** hold the mqtt message sequence numbers **/
CREATE TABLE mqtt_sequence_number (
  bridge_code text,                 //bridgeCode
  value    bigint,                  //value
  time     timestamp,               //timestamp 
  PRIMARY KEY(bridge_code,time)
);

/** hold the things quantity for each zone **/
CREATE TABLE zone_count (
  zone_name text,             //zoneName
  things_quantity counter,    //thingsQuantity
  PRIMARY KEY ((zone_name))
);

/* for sharaf exit_report */
CREATE TABLE exit_report (
  thingType_id bigint,
  group_id     bigint,
  thing_id     bigint,
  time         timestamp,
  zone         varchar,
  value        text,
  PRIMARY KEY(thing_id)
);

CREATE INDEX ON exit_report( thingType_id );
CREATE INDEX ON exit_report( group_id );
CREATE INDEX ON exit_report( time );
CREATE INDEX ON exit_report( zone );

